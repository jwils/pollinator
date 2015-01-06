package com.clover.push.server.client;

import com.clover.push.client.PushClient;
import com.clover.push.message.PushMessage;

/**
 * User: josh
 * Date: 1/3/14
 */
public interface PushClientListener extends PushClient {
    public void onConnect(PushConnection connection);

    public void onMessage(PushMessage message);

    public void onWriteSuccess(PushMessage message);

    public void onWriteFail(PushMessage message, Throwable cause);

    public void onDisconnect();
}
