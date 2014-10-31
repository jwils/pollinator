package com.clover.push.client;

import com.clover.push.message.PushMessage;

/**
 * User: josh
 * Date: 1/3/14
 */
public interface PushClient {
  public String id();

  public boolean isConnected();

  public void onConnect(PushConnection connection);

  public void onMessage(PushMessage message);

  public void onWriteSuccess(PushMessage message);

  public void onWriteFail(PushMessage message, Throwable cause);

  public void onDisconnect();

  public void disconnect();

  public void shutdown();
}
