package com.clover.push.client;

import com.clover.push.PushException;
import com.clover.push.exception.PushConnectionException;
import com.clover.push.message.PushMessage;
import com.clover.push.redis.RedisPushUtils;
import com.clover.push.util.Base64;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.SocketFactory;

public class WebSocketConnection implements PushConnection {
    private static final Random RANDOM = new Random(System.currentTimeMillis());
    private static final String WEBSOCKET_VERSION = "13";
    private static final String WEBSOCKET_GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";

    private final ExecutorService exec = Executors.newSingleThreadExecutor();

    private final String id;
    private final Map<String, String> headers;
    private final Listener callback;
    private HybiParser mParser;
    private SocketFactory socketFactory = null;
    public Socket socket;

    public WebSocketConnection(String id, SocketFactory socketFactory, Map<String, String> headers, Listener callback) {
        this.id = id;
        this.socketFactory = socketFactory;
        this.headers = headers;
        this.callback = callback;
    }
    public WebSocketConnection(String id, Map<String, String> headers, Listener callback) {
        this(id, SocketFactory.getDefault(), headers, callback);
    }


    @Override
    public void writeMessage(PushMessage message) {
        send(RedisPushUtils.encodeMessage(message));
    }

    @Override
    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    @Override
    public void disconnect() {
        mParser.close(0, "Disconnect");
    }

    @Override
    public void disconnect(int reason, String reasonText) {
        mParser.close(reason, reasonText);
    }

    private Map<String, String> getWebSocketUpgradeHeaders(String host, String webSocketSecret) {
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("Upgrade", "websocket");
        headers.put("Connection", "Upgrade");
        headers.put("Host", host);
        headers.put("Sec-WebSocket-Key", webSocketSecret);
        headers.put("Sec-WebSocket-Version", WEBSOCKET_VERSION);
        headers.put("X-CLIENT-ID", id);
        return headers;
    }

    private void writeHeaders(PrintWriter out, Map<String, String> headers) {
        for (Map.Entry<String, String> header : headers.entrySet()) {
            out.print(header.getKey() + ": " + header.getValue() + "\r\n");
        }
        out.print("\r\n");
        out.flush();
    }

    public void connect(URL url) throws PushException {
        int port = url.getPort() != -1 ? url.getPort() : (url.getProtocol().equals("https") ? 443 : 80);

        String path = url.getPath();

        try {
            socket = socketFactory.createSocket();
            socket.setSoTimeout(PushClientConfig.READ_TIMEOUT);
            socket.connect(new InetSocketAddress(url.getHost(), port), PushClientConfig.CONNECT_TIMEOUT);
        } catch (IOException e) {
            throw new PushConnectionException(e);
        }

        String secret = createSecret();
        Map<String, String> connectHeaders = new HashMap<String, String>();
        if (headers != null) {
            connectHeaders.putAll(headers);
        }
        connectHeaders.putAll(getWebSocketUpgradeHeaders(url.getHost(), secret));

        try {
            PrintWriter out = new PrintWriter(socket.getOutputStream());


            out.print("GET " + path + " HTTP/1.1\r\n");
            writeHeaders(out, connectHeaders);

            HybiParser.HappyDataInputStream stream = new HybiParser.HappyDataInputStream(socket.getInputStream());
            String responseStatus = readLine(stream);
            if (responseStatus == null) {
                throw new PushConnectionException("No response from server.");
            }
            String[] responseStatusParts = responseStatus.split(" ");
            if (Integer.parseInt(responseStatusParts[1]) != 101) {
                throw new PushConnectionException("unexpected response from server: " + responseStatus);
            }


            String header;
            while ((header = readLine(stream)) != null && header.length() != 0) {
                String[] parts = header.split(": ");
                if ("Sec-WebSocket-Accept".equalsIgnoreCase(parts[0])) {
                    String expected = expectedKey(secret);
                    if (!expected.equals(parts[1])) {
                        throw new PushConnectionException("Invalid Sec-WebSocket-Accept, expected: " + expected + ", got: " + parts[1]);
                    }
                }
            }

            callback.onConnect();
            mParser = new HybiParser(this);
            mParser.start(stream);
        } catch (SocketException e) {
            throw new PushException(e);
        } catch (IOException e) {
            throw new PushException(e);
        } finally {
            try {
                if (socket != null) socket.close();
            } catch (IOException ignore) {}
        }
    }

    public Listener getListener() {
        return callback;
    }


    private String readLine(HybiParser.HappyDataInputStream reader) throws IOException {
        int readChar = reader.read();
        if (readChar == -1) {
            return null;
        }
        StringBuilder string = new StringBuilder("");
        while (readChar != '\n') {
            if (readChar != '\r') {
                string.append((char) readChar);
            }

            readChar = reader.read();
            if (readChar == -1) {
                return null;
            }
        }
        return string.toString();
    }

    private String createSecret() {
        byte[] nonce = new byte[16];
        RANDOM.nextBytes(nonce);
        return Base64.encodeToString(nonce, Base64.DEFAULT).trim();
    }

    public void send(String data) {
        sendFrame(mParser.frame(data));
    }

    public void send(byte[] data) {
        sendFrame(mParser.frame(data));
    }

    public void close() {
        if (mParser != null) {
            mParser.close(0, "ack close");
        }
    }


    void sendFrame(final byte[] frame) {
        exec.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (socket.isConnected()) {
                        OutputStream outputStream = socket.getOutputStream();
                        outputStream.write(frame);
                        outputStream.flush();
                    }
                } catch (IOException e) {
                    callback.onError(e);
                } catch (Exception e) {
                    callback.onError(e);
                }
            }
        });
    }


    private String expectedKey(String secret) {
        //concatenate, SHA1-hash, base64-encode
        try {
            final String secretGUID = secret + WEBSOCKET_GUID;
            MessageDigest md = MessageDigest.getInstance("SHA-1");
            byte[] digest = md.digest(secretGUID.getBytes());
            return Base64.encodeToString(digest, Base64.DEFAULT).trim();
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    public interface Listener {
        public void onConnect();

        public void onMessage(String message);

        public void onMessage(byte[] data);

        public void onPing(byte[] pingData);

        public void onDisconnect(int code, String reason);

        public void onError(Exception error);
    }
}
