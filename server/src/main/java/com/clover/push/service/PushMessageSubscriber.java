package com.clover.push.service;

import com.clover.push.client.PushClient;
import com.clover.push.message.AckMessage;
import com.clover.push.message.PushMessage;

/**
 * User: josh
 * Date: 1/3/14
 */
public interface PushMessageSubscriber {

  public void start();

  public void stop();

  public boolean isRunning();

  public void registerClient(PushClient client);

  public void removeClient(String clientId);

  public void ackMessage(String clientId, AckMessage message);
}
