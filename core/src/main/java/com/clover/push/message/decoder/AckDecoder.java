package com.clover.push.message.decoder;

import com.clover.push.message.AckMessage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageDecoder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.List;

/**
 * User: josh
 * Date: 1/26/14
 */
public class AckDecoder extends MessageToMessageDecoder<ByteBuf> {
  private static final Logger logger = LoggerFactory.getLogger(AckDecoder.class);
  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf msg, List<Object> out) throws Exception {
    String message = msg.toString(Charset.forName("utf-8"));
    String[] parts = message.split("\n");
    for (String part : parts) {
      if (part.trim().length() > 0) {
        lineToMessage(part, out);
      }
    }
  }

  public void lineToMessage(String line, List<Object> out) {
    String[] parts = line.split(":");
    if ("ack".equalsIgnoreCase(parts[0].trim()) && parts.length == 2) {
      if (isInteger(parts[1])) {
        out.add(new AckMessage(Long.parseLong(parts[1])));
      } else {
        out.add(new AckMessage(parts[1]));
      }
    } else {
      logger.error("Unknown ack message:" + line);
      out.add(new AckMessage("Unknown"));
    }
  }
  public static boolean isInteger(String str)
  {
    return str.matches("\\d+");
  }
}
