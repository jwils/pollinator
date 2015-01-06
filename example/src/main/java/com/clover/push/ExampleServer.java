package com.clover.push;

import com.clover.push.PushServer;
import com.clover.push.server.netty.NettyPushServer;
import org.apache.commons.pool.impl.GenericObjectPool;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;

public class ExampleServer {
    public static void main(String args[]) {
        final GenericObjectPool.Config conf = new GenericObjectPool.Config();
        final ArrayList<JedisPool> pools = new ArrayList<JedisPool>();
        pools.add(new JedisPool(conf, "localhost", 6379, 1000, null));
        PushServer server = new NettyPushServer(pools);
        try {
            server.start();
            Thread.sleep(1000000);
        } catch (InterruptedException ignore) {
        } finally {
            server.start();
        }
    }
}
