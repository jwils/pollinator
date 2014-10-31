package com.clover.push.client;

import com.clover.push.message.PushMessage;

/**
 * User: josh
 * Date: 1/7/14
 */
public class CloverPushGroup implements PushGroup {
  @Override
  public String name() {
    return null;
  }

  @Override
  public void disconnect() {

  }

  @Override
  public void offerClient(PushClient client) {

  }

  @Override
  public void write(PushMessage message) {

  }

  @Override
  public void removeClient(PushClient client) {

  }
}
