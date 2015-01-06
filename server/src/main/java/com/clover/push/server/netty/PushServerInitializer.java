package com.clover.push.server.netty;

import com.clover.push.server.netty.handler.PushMessageHandler;
import com.clover.push.server.netty.handler.WebSocketHandler;
import com.clover.push.server.service.PushMessageSubscriber;

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
    private final long connectionTimeLimitSec;
    private final int pingIntervalSec;
    private final PushMessageSubscriber pushMessageSubscriber;

    public PushServerInitializer(EventExecutorGroup eventExecutors,
                                 PushMessageSubscriber pushMessageSubscriber,
                                 long connectionTimeLimitSec,
                                 int pingIntervalSec,
                                 Random rnd) {
        this.eventExecutors = eventExecutors;
        this.rnd = rnd;
        this.pushMessageSubscriber = pushMessageSubscriber;
        this.connectionTimeLimitSec = connectionTimeLimitSec;
        this.pingIntervalSec = pingIntervalSec;
    }

    @Override
    protected void initChannel(SocketChannel socketChannel) throws Exception {
        ChannelPipeline pipeline = socketChannel.pipeline();

        int timeout = (int) Math.round(connectionTimeLimitSec * ((rnd.nextDouble()) / 5 + 0.9)); // idleTimerBase +/- randomly 10% - to balance the reconnections
        int pingIntervalRandomized = (int) Math.round(pingIntervalSec * ((rnd.nextDouble()) / 5 + 0.9));
        // http stuff
        pipeline.addLast("pingHandler", new IdleStateHandler((int) (pingIntervalSec * 1.1 + 3), pingIntervalRandomized, 0));
        pipeline.addLast("httpCodec", new HttpServerCodec());
        pipeline.addLast("chunkAggregator", new HttpObjectAggregator(512));

        pipeline.addLast("connectionControl", new WebSocketHandler(timeout));
        pipeline.addLast(eventExecutors, "customHandler", new PushMessageHandler(pushMessageSubscriber)); // actual handler
    }
}
