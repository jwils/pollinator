package com.clover.push.message;

/**
 * User: josh
 * Date: 1/3/14
 */
public interface PushMessage {

    public Long getId();

    public String getAppId();

    public Event getEvent();

    public String getData();
}
