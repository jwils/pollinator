package com.clover.push.server.client;

import com.clover.push.message.PushMessage;

/**
 * User: josh
 * Date: 1/3/14
 */
public interface PushConnection {
    public void writeMessage(PushMessage message);

    public boolean isConnected();

    public void disconnect();

    public void disconnect(int reason, String reasonText);
}
