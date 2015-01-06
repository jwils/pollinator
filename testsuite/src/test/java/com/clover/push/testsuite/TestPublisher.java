package com.clover.push.testsuite;

import com.clover.push.PushMessagePublisher;
import com.clover.push.publisher.RedisPushPublisher;
import io.netty.util.concurrent.DefaultEventExecutorGroup;

public class TestPublisher {
    public static PushMessagePublisher getInstance() {
        return new RedisPushPublisher(new DefaultEventExecutorGroup(1), JedisPools.getSingleInstance());
    }
}
