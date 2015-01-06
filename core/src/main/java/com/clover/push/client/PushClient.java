package com.clover.push.client;

import com.clover.push.exception.PushConnectionException;

public interface PushClient {
    public String id();

    public void connect();

    public boolean isConnected();

    public void disconnect();

    public void shutdown();
}
