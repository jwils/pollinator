package com.clover.push.service.redis;

import com.clover.push.client.PushClient;
import com.clover.push.message.AckMessage;
import com.clover.push.message.PushMessage;
import com.clover.push.redis.RedisPushUtils;
import com.clover.push.service.PushMessageSubscriber;

import com.google.common.collect.Lists;

import java.util.List;

import redis.clients.jedis.JedisPool;


/**
 * User: josh
 * Date: 1/3/14
 */
public class RedisPushMessageSubscriber implements PushMessageSubscriber {
  private List<RedisPoolSubscriber> poolSubscribers;
  private List<JedisPool> pools;
  private final int numberOfPools;
  private final int subscribersPerPool;

  public RedisPushMessageSubscriber(List<JedisPool> pools, int subscribersPerPool) {
    this.numberOfPools = pools.size();
    this.subscribersPerPool = subscribersPerPool;
    this.pools = pools;
  }

  public synchronized void start() {
    if (isRunning()) {
      return;
    }

    poolSubscribers = Lists.newArrayList();

    for (int i = 0; i < numberOfPools; i++) {
      for (int s = 0; s < subscribersPerPool; s++) {
        RedisPoolSubscriber subscriber = new RedisPoolSubscriber(pools.get(i), i);
        poolSubscribers.add(subscriber);
      }
    }

    for (RedisPoolSubscriber subscriber : poolSubscribers) {
      subscriber.start();
    }
  }

  public synchronized void stop() {
    if (!isRunning()) {
      return;
    }

    for (RedisPoolSubscriber subscriber : poolSubscribers) {
      subscriber.stop();
    }
  }

  @Override
  public boolean isRunning() {
    if (poolSubscribers == null) {
      return false;
    }
    for (RedisPoolSubscriber subscriber : poolSubscribers) {
      if (subscriber.isRunning()) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void registerClient(PushClient client) {
    int poolId = RedisPushUtils.selectPool(client.id(), numberOfPools);
    //Select a random subscriber. With 1 subscriber per pool this will always return 0.
    int subscriberId = RedisPushUtils.selectPool(client.id() + "RND", subscribersPerPool);

    poolSubscribers.get(poolId * subscribersPerPool + subscriberId).registerClient(client);
  }

  @Override
  public void removeClient(String clientId) {
    int poolId = RedisPushUtils.selectPool(clientId, numberOfPools);
    //Select a random subscriber. With 1 subscriber per pool this will always return 0.
    int subscriberId = RedisPushUtils.selectPool(clientId + "RND", subscribersPerPool);

    poolSubscribers.get(poolId * subscribersPerPool + subscriberId).removeClient(clientId);
  }

  @Override
  public void ackMessage(final String clientId, final AckMessage message) {
    int poolId = RedisPushUtils.selectPool(clientId, numberOfPools);
    //Select a random subscriber. With 1 subscriber per pool this will always return 0.
    int subscriberId = RedisPushUtils.selectPool(clientId + "RND", subscribersPerPool);

    poolSubscribers.get(poolId * subscribersPerPool + subscriberId).ackMessage(clientId, message);
  }
}
