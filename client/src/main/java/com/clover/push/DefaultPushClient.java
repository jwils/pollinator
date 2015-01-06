package com.clover.push;

import com.clover.push.client.PushClient;
import com.clover.push.client.PushClientListener;
import com.clover.push.client.WebSocketConnection;
import com.clover.push.message.AckMessage;
import com.clover.push.message.PushMessage;
import com.clover.push.redis.RedisPushUtils;
import com.clover.push.util.Ids;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class DefaultPushClient implements PushClient, WebSocketConnection.Listener {
    private final String id;
    private final URL hostUrl;
    private WebSocketConnection connection;
    private PushClientListener delegate;
    private final ExecutorService exec = Executors.newSingleThreadExecutor();

    private long lastSeen = -1;


    public DefaultPushClient(String clientId, String serverHost, int serverPort, String pushPath, PushClientListener delegate) {
        this.id = clientId;
        try {
            this.hostUrl = new URL("http", serverHost, serverPort, pushPath);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        this.delegate = delegate;
    }

    public DefaultPushClient(String serverHost, int serverPort, String pushPath, PushClientListener delegate) {
        this(Ids.toUUID(Ids.uuid128()), serverHost, serverPort, pushPath, delegate);
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public void connect() {
        this.connection = new WebSocketConnection(id, null, this);
        exec.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    DefaultPushClient.this.connection.connect(hostUrl);
                } catch (PushException e) {
                    onError(e);
                } catch (Exception e) {
                    e.printStackTrace();
                    onError(e);
                }
            }
        });
    }

    @Override
    public boolean isConnected() {
        return connection.isConnected();
    }

    @Override
    public void disconnect() {
        connection.close();
    }

    @Override
    public void shutdown() {
        connection.close();
    }

    @Override
    public void onConnect() {
        delegate.onConnect(connection);
    }

    @Override
    public void onMessage(String message) {
        PushMessage msg = RedisPushUtils.decodeMessage(message);
        if (msg != null && (msg.getId() == null || msg.getId() > lastSeen)) {
            if (msg.getId() != null) {
                lastSeen = msg.getId();
                connection.writeMessage(new AckMessage(msg.getId()));
            } else {
                connection.writeMessage(new AckMessage(msg.getEvent()));
            }

            delegate.onMessage(msg);
        }
    }

    @Override
    public void onMessage(byte[] data) {
        //Binary websocket data is not supported.
        disconnect();
    }

    @Override
    public void onPing(byte[] pingData) {
    }

    @Override
    public void onDisconnect(int code, String reason) {
        delegate.onDisconnect();
    }

    @Override
    public void onError(Exception error) {
        error.printStackTrace();
    }
}
