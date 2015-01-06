package com.clover.push.server.netty;

import com.clover.push.PushServer;
import com.clover.push.server.PushServerConfig;
import com.clover.push.server.service.PushMessageSubscriber;
import com.clover.push.server.service.redis.RedisPushMessageSubscriber;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.EventExecutorGroup;
import io.netty.util.internal.logging.InternalLoggerFactory;
import io.netty.util.internal.logging.Log4JLoggerFactory;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import redis.clients.jedis.JedisPool;

public class NettyPushServer implements PushServer {
    private static final Logger logger = LoggerFactory.getLogger(NettyPushServer.class);
    private static final Random RND = new Random();
    private final List<JedisPool> redisPools;
    ChannelFuture serverFuture;


    public NettyPushServer(List<JedisPool> redisPools) {
        this.redisPools = redisPools;
    }

    // thread pool
    private EventExecutorGroup executors;
    private PushMessageSubscriber pushMessageSubscriber;

    public void start() {
        // Set log4j as default logger for netty
        InternalLoggerFactory.setDefaultFactory(new Log4JLoggerFactory());

        executors = new DefaultEventExecutorGroup(8);
        pushMessageSubscriber = new RedisPushMessageSubscriber(redisPools, 1);


        ChannelInitializer pushInitializer =
                new PushServerInitializer(executors,
                        pushMessageSubscriber,
                        PushServerConfig.CLIENT_FORCE_DISCONNECT_TIME_SEC,
                        PushServerConfig.KEEP_ALIVE_INTERVAL_SEC,
                        RND);


        pushMessageSubscriber.start();
        serverFuture = new ServerBootstrap().channel(NioServerSocketChannel.class).group(new NioEventLoopGroup(4)).childHandler(pushInitializer).bind(8013);
    }

    public void stop() {
        pushMessageSubscriber.stop();
        try {
            Thread.sleep(500);
        } catch (InterruptedException ignore) {}
        executors.shutdownGracefully();
    }

    public static void main(String[] args) {
        final GenericObjectPool.Config conf = new GenericObjectPool.Config();
        final ArrayList<JedisPool> pools = new ArrayList<JedisPool>();
        pools.add(new JedisPool(conf, "localhost", 6379, 1000, null));

        new NettyPushServer(pools).start();
    }
}
