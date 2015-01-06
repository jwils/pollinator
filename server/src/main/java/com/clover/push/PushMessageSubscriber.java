package com.clover.push;

import com.clover.push.message.AckMessage;

/**
 * User: josh
 * Date: 1/3/14
 */
public interface PushMessageSubscriber {

    public void start();

    public void stop();

    public boolean isRunning();

    public void registerClient(PushClientListener client);

    public void removeClient(String clientId);

    public void ackMessage(String clientId, AckMessage message);
}
