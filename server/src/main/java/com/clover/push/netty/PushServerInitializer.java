package com.clover.push.netty;

import com.clover.push.handler.PushMessageHandler;
import com.clover.push.handler.WebSocketHandler;
import com.clover.push.service.PushMessageSubscriber;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.EventExecutorGroup;

import java.util.Random;
/**
 * User: josh
 * Date: 12/16/13
 */
class PushServerInitializer extends ChannelInitializer<SocketChannel> {
  private final EventExecutorGroup eventExecutors;
  private final Random rnd;
  private final long idleTimeout;
  private final int pingIntervalSec;
  private final PushMessageSubscriber pushMessageSubscriber;

  public PushServerInitializer(EventExecutorGroup eventExecutors,
                               PushMessageSubscriber pushMessageSubscriber,
                               long idleTimeout,
                               int pingIntervalSec,
                               Random rnd) {
    this.eventExecutors = eventExecutors;
    this.rnd = rnd;
    this.pushMessageSubscriber = pushMessageSubscriber;
    this.idleTimeout = idleTimeout;
    this.pingIntervalSec = pingIntervalSec;
  }

  @Override
  protected void initChannel(SocketChannel socketChannel) throws Exception {
    ChannelPipeline pipeline = socketChannel.pipeline();

    int timeout = (int) Math.round(idleTimeout * ((rnd.nextDouble()) / 5 + 0.9)); // idleTimerBase +/- randomly 10% - to balance the reconnections
    int pingIntervalRandomized = (int)Math.round(pingIntervalSec * ((rnd.nextDouble()) / 5 + 0.9));
    // http stuff
    pipeline.addLast("idleTimeout", new IdleStateHandler((int)(pingIntervalSec * 1.1 + 3), pingIntervalRandomized, 0)); //Config has ping interval in ms.
    pipeline.addLast("httpCodec", new HttpServerCodec());
    pipeline.addLast("chunkAggregator", new HttpObjectAggregator(512));

    pipeline.addLast("connectionControl", new WebSocketHandler(timeout));
    pipeline.addLast(eventExecutors, "customHandler", new PushMessageHandler(pushMessageSubscriber)); // actual handler
  }
}
