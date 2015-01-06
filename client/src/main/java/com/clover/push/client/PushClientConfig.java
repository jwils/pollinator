package com.clover.push.client;

public class PushClientConfig {
    public static final int CONNECT_TIMEOUT = 30 * 1000; // 30s
    // This value must be larger than the keep-alive interval set by the server.
    public static final int READ_TIMEOUT = 5 * 60 * 1000; // 5min
}
