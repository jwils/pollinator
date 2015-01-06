package com.clover.push.client;

import com.clover.push.message.PushMessage;

/**
 * User: josh
 * Date: 1/3/14
 */
public interface PushClientListener {
    public void onConnect(PushConnection connection);

    public void onMessage(PushMessage message);

    public void onDisconnect();
}
