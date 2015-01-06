package com.clover.push.server.handler;

import com.clover.push.server.client.DefaultPushClientListener;
import com.clover.push.server.client.NettyPushConnection;
import com.clover.push.server.client.PushClientListener;
import com.clover.push.message.AckMessage;
import com.clover.push.message.ConnectMessage;
import com.clover.push.redis.RedisPushUtils;
import com.clover.push.PushMessageSubscriber;

import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandlerContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PushMessageHandler extends ChannelDuplexHandler {
    private static final Logger logger = LoggerFactory.getLogger(PushMessageHandler.class);
    public final PushMessageSubscriber pushMessageSubscriber;

    public PushMessageHandler(PushMessageSubscriber subscriber) {
        this.pushMessageSubscriber = subscriber;
    }

    private PushClientListener client;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ConnectMessage) {
            ConnectMessage connectMessage = (ConnectMessage) msg;
            handleRegister(ctx, connectMessage);
        } else if (msg instanceof String) {
            logger.info("device sent message" + msg);
            AckMessage ack = RedisPushUtils.decodeAckMessage((String) msg);
            if (ack == null) {
                logger.info("Unable to decode: {}", msg);
                return;
            }
            pushMessageSubscriber.ackMessage(client.id(), ack);
        } else {
            logger.info("Unknown message class {}", msg.getClass().getName());
        }

    }

    public void handleRegister(ChannelHandlerContext ctx, ConnectMessage msg) {

        client = new DefaultPushClientListener(msg.getClientId(), pushMessageSubscriber);
        client.onConnect(new NettyPushConnection(client, ctx));

        pushMessageSubscriber.registerClient(client);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) {
        pushMessageSubscriber.removeClient(client.id());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("Error in message pipeline", cause);
    }

}
