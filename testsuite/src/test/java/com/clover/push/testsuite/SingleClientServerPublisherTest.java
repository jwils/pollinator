package com.clover.push.testsuite;

import com.clover.push.DefaultPushClient;
import com.clover.push.PushMessagePublisher;
import com.clover.push.client.PushClientListener;
import com.clover.push.client.PushConnection;
import com.clover.push.message.DefaultPushMessage;
import com.clover.push.message.Event;
import com.clover.push.PushServer;
import com.clover.push.message.PushMessage;
import org.junit.Test;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.junit.Assert.assertTrue;

public class SingleClientServerPublisherTest {
    @Test
    public void testSingleMessageAcrossPublisherServerClient() throws InterruptedException {
        final PushServer server = TestServer.getInstance();
        server.start();

        final PushMessagePublisher publisher = TestPublisher.getInstance();
        publisher.start();
        final AtomicBoolean passed = new AtomicBoolean(false);
        DefaultPushClient client = new DefaultPushClient("TEST", "localhost", 8013, "/push", new PushClientListener() {
            @Override
            public void onConnect(PushConnection connection) {

            }

            @Override
            public void onMessage(PushMessage message) {
                passed.set(true);
            }

            @Override
            public void onDisconnect() {

            }
        });


        client.connect();
        publisher.enqueueMessage("TEST", new DefaultPushMessage(new Event("testEvent"), "Message", "TEST"));
        Thread.sleep(10000);
        server.stop();
        assertTrue("Client should receive test message.", passed.get());
    }
}
