package com.clover.push.message;

/**
 * User: josh
 * Date: 1/29/14
 */
public class ConnectMessage {
    private final String clientId;

    public ConnectMessage(String clientId) {
        this.clientId = clientId;
    }

    public String getClientId() {
        return clientId;
    }
}
