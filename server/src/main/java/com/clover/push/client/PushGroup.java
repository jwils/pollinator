package com.clover.push.client;


import com.clover.push.message.PushMessage;

/**
 * User: josh
 * Date: 1/3/14
 */
public interface PushGroup {
  public String name();

  public void disconnect();

  public void offerClient(PushClient client);

  public void write(PushMessage message);

  public void removeClient(PushClient client);


}
