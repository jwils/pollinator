package com.clover.push.server.service;

import com.clover.push.message.AckMessage;
import com.clover.push.server.client.ServerClientListener;

/**
 * User: josh
 * Date: 1/3/14
 */
public interface PushMessageSubscriber {

    public void start();

    public void stop();

    public boolean isRunning();

    public void registerClient(ServerClientListener client);

    public void removeClient(String clientId);

    public void ackMessage(String clientId, AckMessage message);
}
