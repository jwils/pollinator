package com.clover.push.testsuite;

import com.clover.push.PushServer;
import com.clover.push.server.netty.NettyPushServer;

public class TestServer {
    public static PushServer getInstance() {
      return new NettyPushServer(JedisPools.getSingleInstance());
    }
}
