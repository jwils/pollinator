package com.clover.push;

import com.clover.push.client.PushClientListener;
import com.clover.push.client.PushConnection;
import com.clover.push.message.PushMessage;

public class ExampleClient {
    public ExampleClient() {}

    public static void main(String args[]) {
        if (args.length != 1) {
            System.out.println("Push client takes one argument which is the client id.");
            return;
        }

        DefaultPushClient client = new DefaultPushClient(args[0], "localhost", 8013, "/push", new PushClientListener() {
            @Override
            public void onConnect(PushConnection connection) {
                System.out.println("successfully connected to server.");
            }

            @Override
            public void onMessage(PushMessage message) {
                System.out.print("received message: ");
                System.out.println("Id: " + message.getId());
                System.out.println("Event: " + message.getEvent().getName());
                System.out.println("Data: " + message.getData());
                System.out.println("AppId: " + message.getAppId());

            }

            @Override
            public void onDisconnect() {

            }
        });
        client.connect();
    }
}
