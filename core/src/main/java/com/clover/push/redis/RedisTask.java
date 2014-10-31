package com.clover.push.redis;

import redis.clients.jedis.Jedis;

public interface RedisTask {
  public void runTask(Jedis conn);
}
