package com.clover.push.server.client;


import com.clover.push.client.PushClient;
import com.clover.push.message.PushMessage;
import com.clover.push.PushMessageSubscriber;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * User: josh
 * Date: 1/3/14
 */
public class DefaultPushClientListener implements PushClientListener, PushClient {
    private static final Logger logger = LoggerFactory.getLogger(DefaultPushClientListener.class);

    private final String id;

    private PushConnection connection;
    private PushMessageSubscriber pushService;

    public DefaultPushClientListener(String id, PushMessageSubscriber pushService) {
        if (pushService == null) {
            throw new NullPointerException("pushService");
        }
        this.pushService = pushService;
        this.id = id;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public boolean isConnected() {
        return connection.isConnected();
    }

    @Override
    public void onConnect(PushConnection conn) {
        if (conn == null) {
            throw new NullPointerException("connection");
        }
        connection = conn;
    }

    @Override
    public void onMessage(PushMessage message) {
        if (connection != null && connection.isConnected()) {
            connection.writeMessage(message);
        } else {
            onWriteFail(message, null); //Client is not connected
        }
    }

    @Override
    public void onWriteSuccess(PushMessage message) {
    }

    @Override
    public void onWriteFail(PushMessage message, Throwable cause) {
        logger.error("Failed to write message", cause);
        connection.disconnect();
    }

    @Override
    public void onDisconnect() {
        pushService.removeClient(id());
    }

    @Override
    public void disconnect() {
        connection.disconnect();
    }

    public void shutdown() {
        connection.disconnect(1001, "Server maintenance.");
    }
}
