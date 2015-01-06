package com.clover.push.testsuite;

import org.apache.commons.pool.impl.GenericObjectPool;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.List;

public class JedisPools {
    public static List<JedisPool> getSingleInstance() {
        final GenericObjectPool.Config conf = new GenericObjectPool.Config();
        final ArrayList<JedisPool> pools = new ArrayList<JedisPool>();
        pools.add(new JedisPool(conf, "localhost", 6379, 1000, null));
        return pools;
    }

    // Simulates two hosts using a single instance.
    public static List<JedisPool> getTwoInstances() {
        final GenericObjectPool.Config conf = new GenericObjectPool.Config();
        final ArrayList<JedisPool> pools = new ArrayList<JedisPool>();
        pools.add(new JedisPool(conf, "localhost", 6379, 1000, null));
        pools.add(new JedisPool(conf, "localhost", 6379, 1000, null));
        return pools;
    }
}
