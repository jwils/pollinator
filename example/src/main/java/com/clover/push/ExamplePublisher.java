package com.clover.push;

import com.clover.push.message.DefaultPushMessage;
import com.clover.push.message.Event;
import com.clover.push.publisher.RedisPushPublisher;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import org.apache.commons.pool.impl.GenericObjectPool;
import redis.clients.jedis.JedisPool;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ExamplePublisher {
    public static void main(String args[]) {
        System.out.println("Starting up.....");
        final GenericObjectPool.Config conf = new GenericObjectPool.Config();
        final ArrayList<JedisPool> pools = new ArrayList<JedisPool>();
        pools.add(new JedisPool(conf, "localhost", 6379, 1000, null));
        PushMessagePublisher publisher = new RedisPushPublisher(new DefaultEventExecutorGroup(1), pools);
        try {
            publisher.start();
            while (true) {
                System.out.println("Enqueue a new message");
                BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
                System.out.print("enter client id: ");
                String clientId = br.readLine();
                System.out.print("Enter event: ");
                String event = br.readLine();
                System.out.print("Enter data: ");
                String data = br.readLine();
                System.out.print("Enter appId: ");
                String appId = br.readLine();
                publisher.enqueueMessage(clientId, new DefaultPushMessage(new Event(event), data, appId));
                System.out.println();
                Thread.sleep(1000);
            }
        } catch (IOException ignore) {
        } catch (InterruptedException ignore) {
        } finally {
            publisher.start();
        }
    }
}
