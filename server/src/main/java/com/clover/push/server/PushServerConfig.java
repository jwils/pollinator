package com.clover.push.server;

public class PushServerConfig {
    //4 and a half min
    public static final int KEEP_ALIVE_INTERVAL_SEC = 4 * 60 + 30;

    // Disconnect clients every 60 min to force them to make a new connection.
    public static final int CLIENT_FORCE_DISCONNECT_TIME_SEC = 60*60;
}
