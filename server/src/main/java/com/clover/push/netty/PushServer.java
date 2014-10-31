package com.clover.push.netty;

import com.clover.push.service.PushMessageSubscriber;
import com.clover.push.service.redis.RedisPushMessageSubscriber;

import io.netty.bootstrap.ServerBootstrap;
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

public class PushServer {
  private static final Logger logger = LoggerFactory.getLogger(PushServer.class);
  private static final Random RND = new Random();
  private final List<JedisPool> redisPools;


  public PushServer(List<JedisPool> redisPools) {
    this.redisPools = redisPools;
  }

  // thread pool
  private EventExecutorGroup executors;
  private PushMessageSubscriber pushMessageSubscriber;

  public void start() {
    // Set log4j as default logger for netty
    InternalLoggerFactory.setDefaultFactory(new Log4JLoggerFactory());

    executors = new DefaultEventExecutorGroup(8);

    int pushKeepAliveSecs = 4*60 + 20;
    int writeTimeout = 60*60;
    pushMessageSubscriber = new RedisPushMessageSubscriber(redisPools, 1);


    ChannelInitializer pushInitializer =
        new PushServerInitializer(executors,
                                  pushMessageSubscriber,
                                  writeTimeout,
                                  pushKeepAliveSecs,
                                  RND);


    pushMessageSubscriber.start();
    new ServerBootstrap().channel(NioServerSocketChannel.class).group(new NioEventLoopGroup(4)).childHandler(pushInitializer).bind(8013).syncUninterruptibly();
  }

  public static void main(String[] args) {
    final GenericObjectPool.Config conf = new GenericObjectPool.Config();
    final ArrayList<JedisPool> pools = new ArrayList<JedisPool>();
    pools.add(new JedisPool(conf, "localhost", 6379, 1000, "test"));

    new PushServer(pools).start();
  }
}
