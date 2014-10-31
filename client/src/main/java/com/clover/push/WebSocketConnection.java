package com.clover.push;

import sun.security.ssl.SSLSocketFactoryImpl;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.net.SocketFactory;

public class WebSocketConnection  {
  private static final int CONNECT_TIMEOUT = 30 * 1000; // 30s
  // This value must be larger than the keep-alive interval set by the server.
  private static final int READ_TIMEOUT = 5 * 60 * 1000; // 5min

  private static final Random RANDOM = new Random(System.currentTimeMillis());
  private static final String WEBSOCKET_VERSION = "13";

  private final ExecutorService exec = Executors.newSingleThreadExecutor();

  private final Map<String, String> headers;
  private final Listener callback;
  private HybiParser mParser;
  private SocketFactory socketFactory = null;
  public Socket socket;

  public WebSocketConnection(SocketFactory socketFactory, Map<String, String> headers, Listener callback) {
    this.socketFactory = socketFactory;
    this.headers = headers;
    this.callback = callback;
  }

  public void connect(URL url) throws Exception {
    int port = url.getPort() != -1 ? url.getPort() : (url.getProtocol().equals("https") ? 443 : 80);
    //local config for push server
    if (port == 9000) {
      port = 8011;
    }

    String path = url.getPath();

    if (socketFactory == null) {
      if (url.getProtocol().equals("https")) {
        //socketFactory = ;
      } else {
        socketFactory = new SSLSocketFactoryImpl();
      }
    }


    socket = socketFactory.createSocket(url.getHost(), port);

    try {
      PrintWriter out = new PrintWriter(socket.getOutputStream());

      String secret = createSecret();
      out.print("GET " + path + " HTTP/1.1\r\n");
      out.print("Upgrade: websocket\r\n");
      out.print("Connection: Upgrade\r\n");
      out.print("Host: " + url.getHost() + "\r\n");
      out.print("Sec-WebSocket-Key: " + secret + "\r\n");
      out.print("Sec-WebSocket-Version: " + WEBSOCKET_VERSION + "\r\n");
      for (Map.Entry<String, String> header : headers.entrySet()) {
        out.print(header.getKey() + ": " + header.getValue() + "\r\n");
      }
      //out.print(CloverHeader.X_CLOVER_AUTH_TOKEN + ": " + merchant.getToken() + "\r\n");
      //out.print(CloverHeader.X_CLOVER_DEVICE + ": " + device.getDeviceId() + "\r\n");
      out.print("\r\n");
      out.flush();

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
    } finally {
      if (socket != null) socket.close();
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
      final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
      final String secretGUID = secret + GUID;
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