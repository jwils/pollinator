package com.clover.push;


import com.clover.push.message.PushMessage;

/**
 * User: josh
 * Date: 1/13/14
 */
public interface PushMessagePublisher {

    public void start();

    public void stop();

    public void enqueueMessage(String clientOrGroup, PushMessage message);

    public void enqueueMessage(String clientOrGroup, PushMessage message, int timeoutSec);
}
