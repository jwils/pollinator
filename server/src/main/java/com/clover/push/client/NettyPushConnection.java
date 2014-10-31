package com.clover.push.client;

import com.clover.push.message.PushMessage;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.CloseWebSocketFrame;

/**
 * User: josh
 * Date: 1/3/14
 */
public class NettyPushConnection implements PushConnection {
  private final PushClient client;

  private ChannelHandlerContext nettyCtx;
  private ChannelFutureListener closeFuture;

  public NettyPushConnection(final PushClient client,
                             ChannelHandlerContext nettyCtx) {
    this.client = client;
    this.nettyCtx = nettyCtx;
    closeFuture = new ChannelFutureListener() {
      @Override
      public void operationComplete(ChannelFuture future) throws Exception {
        client.onDisconnect();
      }
    };

    nettyCtx.channel().closeFuture().addListener(closeFuture);
  }

  @Override
  public void writeMessage(final PushMessage message) {
    Channel channel = nettyCtx.channel();
    channel.write(message).addListener(new MessageWriteFuture(message));
    channel.flush();
  }

  @Override
  public boolean isConnected() {
    return nettyCtx.channel().isWritable();
  }


  public void disconnect(int reason, String reasonText) {
    Channel channel = nettyCtx.channel();
    channel.closeFuture().removeListener(closeFuture);
    nettyCtx.writeAndFlush(new CloseWebSocketFrame(reason, reasonText)).addListener(ChannelFutureListener.CLOSE);
  }

  @Override
  public void disconnect() {
    disconnect(1000, "Normal. Please reconnect.");
  }

  private class MessageWriteFuture implements ChannelFutureListener {
    private final PushMessage message;
    public MessageWriteFuture(PushMessage message) {
      this.message = message;
    }

    @Override
    public void operationComplete(ChannelFuture future) throws Exception {
      if (future.isSuccess()) {
        client.onWriteSuccess(message);
      } else {
        client.onWriteFail(message, future.cause());
      }
    }
  }
}
