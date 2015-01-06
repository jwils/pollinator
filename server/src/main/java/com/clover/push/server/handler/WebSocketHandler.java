package com.clover.push.server.handler;

import com.clover.push.message.encoder.SSEPushEncoder;
import com.clover.push.message.encoder.WebSocketPushEncoder;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PingWebSocketFrame;
import io.netty.handler.codec.http.websocketx.PongWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import io.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.channels.ClosedChannelException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * User: josh
 * Date: 1/29/14
 */
public class WebSocketHandler extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketHandler.class);
    private static final WebSocketServerHandshakerFactory wsFactory =
            new WebSocketServerHandshakerFactory(getWebSocketLocation(), null, false);

    private final int clientConnectionTimeoutSecs;
    private Future closeFuture = null;
    private ScheduledFuture<?> closeConnection = null;
    WebSocketServerHandshaker handshaker;

    private boolean connected = false;
    private boolean handshakeComplete = false;

    public WebSocketHandler(int clientConnectionTimeoutSecs) {
        this.clientConnectionTimeoutSecs = clientConnectionTimeoutSecs;
    }


    @Override
    public void channelRegistered(final ChannelHandlerContext ctx) throws Exception {
        ctx.pipeline().addBefore(ctx.name(), "pushMessageEncoder", new SSEPushEncoder());
        closeConnection = ctx.executor().schedule(new CloseChannelRunnable(ctx),
                clientConnectionTimeoutSecs, TimeUnit.SECONDS);
    }

    public void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest request) {
        if (connected) {
            logger.error("client is already connected. Force disconnecting");
            ctx.close();
        }
        if (isWebsocketUpgradeRequest(request)) {
            handleWebsocketUpgrade(ctx, request);
        } else {
            logger.error("Unsupported request");
            ctx.close();
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (closeFuture != null) {
            closeFuture.cancel(false);
            closeFuture = null;
        }
        if (msg instanceof FullHttpRequest) {
            handleHttpRequest(ctx, (FullHttpRequest) msg);
        } else if (msg instanceof WebSocketFrame) {
            handleWebsocketRequest(ctx, (WebSocketFrame) msg);
        } else {
            logger.warn("unhandled message type: " + msg.getClass().getName());
            ReferenceCountUtil.release(msg);
        }
    }

    private void handleWebsocketRequest(ChannelHandlerContext ctx, WebSocketFrame msg) {
        try {
            if (msg instanceof CloseWebSocketFrame) {
                handshaker.close(ctx.channel(), (CloseWebSocketFrame) msg.retain());
                connected = false;
            } else if (msg instanceof PingWebSocketFrame) {
                ctx.channel().writeAndFlush(new PongWebSocketFrame());
            } else if (msg instanceof PongWebSocketFrame) {
                logger.debug("Pong.");
            } else if (msg instanceof TextWebSocketFrame) {
                String clientMessage = ((TextWebSocketFrame) msg).text();
                if (clientMessage == null) {
                    logger.debug("Client sent empty message.");
                } else {
                    ctx.fireChannelRead(clientMessage);
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    private boolean isWebsocketUpgradeRequest(HttpRequest httpRequest) {
        HttpHeaders headers = httpRequest.headers();
        return headers.get(HttpHeaders.Names.UPGRADE) != null &&
                HttpHeaders.Values.WEBSOCKET.equalsIgnoreCase(headers.get(HttpHeaders.Names.UPGRADE));
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        destroy();
        super.channelInactive(ctx);
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        destroy();
        super.handlerRemoved(ctx);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            if (!connected) {
                logger.info("Client is not yet connected");
                return;
            }
            idleStateHandler(ctx, (IdleStateEvent) evt);
        } else {
            ctx.fireUserEventTriggered(evt);
        }
    }

    private void handleWebsocketUpgrade(final ChannelHandlerContext ctx, final FullHttpRequest req) {
        ctx.pipeline().replace("pushMessageEncoder", "pushMessageEncoder", new WebSocketPushEncoder());

        // Handshake

        handshaker = wsFactory.newHandshaker(req);
        if (handshaker == null) {
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        } else {
            handshaker.handshake(ctx.channel(), req).addListener(new ChannelFutureListener() {
                @Override
                public void operationComplete(ChannelFuture future) throws Exception {
                    if (future.isSuccess()) {
                        connected = true;
                        handshakeComplete = true;
                        sendKeepAlive(ctx);
                        ctx.fireChannelRegistered();
                        ctx.fireChannelRead(req);
                    } else {
                        logger.error("Unable to successfully handshake", future.cause());
                    }
                }
            });
        }
    }

    private static String getWebSocketLocation() {
        return "wss://api.clover.com/sockets";
    }

    public void idleStateHandler(final ChannelHandlerContext ctx, IdleStateEvent event) {
        switch (event.state()) {
            case READER_IDLE:
                sendKeepAlive(ctx);
                break;
            case WRITER_IDLE:
                //sendKeepAlive(ctx);
                //break;
            case ALL_IDLE:
                //Not configured.
                break;
        }
        ctx.flush();
    }

    @Override
    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (handshakeComplete && !connected) {
            promise.setFailure(new ClosedChannelException());
        } else {
            ctx.write(msg, promise);
        }
    }

    private void sendKeepAlive(final ChannelHandlerContext ctx) {
        ctx.write(new PingWebSocketFrame());
        if (closeFuture != null) {
            closeFuture = ctx.executor().schedule(new CloseChannelRunnable(ctx), 10, TimeUnit.SECONDS);
        }
    }

    public void sendClose(ChannelHandlerContext ctx) {
        logger.info("Closing idle channel");
        ChannelFuture future = ctx.write(new CloseWebSocketFrame());

        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                if (!future.isSuccess()) {
                    Throwable cause = future.cause();
                    if (cause instanceof ClosedChannelException) {
                        logger.info("Attempting to write to closed channel");
                    } else {
                        logger.info("Unknown error closing channel", cause);
                    }
                }
            }
        });
        ctx.flush();
    }


    private void destroy() {
        if (closeConnection != null) {
            closeConnection.cancel(false);
            closeConnection = null;
        }

        if (closeFuture != null) {
            closeFuture.cancel(false);
            closeFuture = null;
        }
    }

    private class CloseChannelRunnable implements Runnable {
        private final ChannelHandlerContext ctx;

        public CloseChannelRunnable(ChannelHandlerContext ctx) {
            this.ctx = ctx;
        }

        @Override
        public void run() {
            connected = false;
            sendClose(ctx);
        }
    }
}
