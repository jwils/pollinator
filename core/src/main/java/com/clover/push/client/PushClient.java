package com.clover.push.client;

public interface PushClient {
    public String id();

    public boolean isConnected();

    public void disconnect();

    public void shutdown();
}
