package com.clover.push.message.encoder;

import com.clover.push.message.PushMessage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * User: josh
 * Date: 1/29/14
 */
public class WebSocketPushEncoder extends PushMessageEncoderHandler {
  private static final Logger logger = LoggerFactory.getLogger(WebSocketPushEncoder.class);
  private static final byte[] LFCR = "\r\n".getBytes(defaultCharacterSet);
  private static final byte[] ID = "\"id\": ".getBytes(defaultCharacterSet);
  private static final byte[] EVENT = "\"event\": ".getBytes(defaultCharacterSet);
  private static final byte[] DATA = "\"data\": ".getBytes(defaultCharacterSet);
  private static final byte[] APPID = "\"appId\": ".getBytes(defaultCharacterSet);
  private static final byte[] comma = ", ".getBytes(defaultCharacterSet);
  @Override
  public Object encode(ChannelHandlerContext ctx, PushMessage message) throws IOException {
    ByteBuf buf = ctx.alloc().buffer().clear();

    buf.writeBytes("{".getBytes(defaultCharacterSet));

    if (message.getId() != null) {
      buf.writeBytes(ID);
      buf.writeBytes("\"".getBytes());
      buf.writeBytes(message.getId().toString().getBytes(defaultCharacterSet));
      buf.writeBytes("\"".getBytes());
      buf.writeBytes(comma);
    }
    buf.writeBytes(EVENT);
    buf.writeBytes("\"".getBytes());
    buf.writeBytes(message.getEvent().getName().getBytes(defaultCharacterSet));
    buf.writeBytes("\"".getBytes());

    if (message.getAppId() != null) {
      buf.writeBytes(comma);
      buf.writeBytes(APPID);
      buf.writeBytes("\"".getBytes());
      buf.writeBytes(message.getAppId().getBytes(defaultCharacterSet));
      buf.writeBytes("\"".getBytes());
    }
    if (message.getData() != null) {
      buf.writeBytes(comma);
      buf.writeBytes(DATA);
      buf.writeBytes(message.getData().getBytes(defaultCharacterSet));
    }
    buf.writeBytes("}".getBytes(defaultCharacterSet));
    TextWebSocketFrame frame = new TextWebSocketFrame(buf);
    logger.debug("Sending frame with data: " + frame.text());
    return frame;
  }
}
