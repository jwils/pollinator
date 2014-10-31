package com.clover.push.publisher;

import com.clover.push.message.PushMessage;
import com.clover.push.PushMessagePublisher;

import com.google.common.collect.Lists;

import io.netty.util.concurrent.EventExecutorGroup;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import redis.clients.jedis.JedisPool;

import static com.clover.push.redis.RedisPushUtils.selectPool;

/**
 * User: josh
 * Date: 1/13/14
 */
public class RedisPushPublisher implements PushMessagePublisher {
  private static final Logger logger = LoggerFactory.getLogger(RedisPushPublisher.class);
  //DEFAULT TIMEOUT 5 days.
  private static final int DEFAULT_TIMEOUT_SEC = 60 * 60 * 24 * 5;
  private static final int PUSH_PUBLISH_INTERVAL_MS = 5000; // 5 Seconds

  private List<BlockingQueue<PushMessageHolder>> messageQueues;
  private List<MessagePoolQueuePublisher> messagePools;
  private int poolCount;

  private EventExecutorGroup executor;

  public RedisPushPublisher(EventExecutorGroup executor, List<JedisPool> pools) {
    this.executor = executor;

    messageQueues = Lists.newArrayList();
    messagePools = Lists.newArrayList();
    for (int i = 0; i < pools.size(); i++) {
      BlockingQueue<PushMessageHolder> queue = new LinkedBlockingDeque<PushMessageHolder>();
      messageQueues.add(queue);
      messagePools.add(new MessagePoolQueuePublisher(queue,pools.get(i), i));
    }

    poolCount = messagePools.size();
    for (int i = 0; i < poolCount; i++) {
      int startDelay = PUSH_PUBLISH_INTERVAL_MS * i/poolCount;
      executor.scheduleAtFixedRate(messagePools.get(i),
                                   startDelay,
                                   PUSH_PUBLISH_INTERVAL_MS,
                                   TimeUnit.MILLISECONDS);
    }
  }

  @Override
  public void enqueueMessage(final String clientOrGroup, final PushMessage message) {
    enqueueMessage(clientOrGroup, message, DEFAULT_TIMEOUT_SEC);
  }

  @Override
  public void enqueueMessage(String corG, final PushMessage message, final int timeoutSec) {
    final String clientOrGroup = corG.toLowerCase();
    offer(clientOrGroup, message, timeoutSec);
  }

  public void offer(String clientOrGroup, PushMessage pushMessage, int timeoutSec) {
    PushMessageHolder holder = new PushMessageHolder();
    holder.clientKey = clientOrGroup;
    holder.message = pushMessage;
    holder.timeoutSec = timeoutSec;
    int poolId = selectPool(clientOrGroup, poolCount);

    messageQueues.get(poolId).offer(holder);
  }
}
