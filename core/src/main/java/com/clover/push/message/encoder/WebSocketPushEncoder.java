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
public class WebSocketPushEncoder extends PushMessageEncoderHandler<TextWebSocketFrame> {
    private static final Logger logger = LoggerFactory.getLogger(WebSocketPushEncoder.class);
    private static final byte[] ID = "\"id\": ".getBytes(getCharacterSet());
    private static final byte[] EVENT = "\"event\": {\"name\": ".getBytes(getCharacterSet());
    private static final byte[] DATA = "\"data\": ".getBytes(getCharacterSet());
    private static final byte[] APPID = "\"appId\": ".getBytes(getCharacterSet());
    private static final byte[] COMMA = ", ".getBytes(getCharacterSet());
    private static final byte[] QUOTE = "\"".getBytes(getCharacterSet());

    @Override
    public TextWebSocketFrame encode(ChannelHandlerContext ctx, PushMessage message) throws IOException {
        ByteBuf buf = ctx.alloc().buffer().clear();

        buf.writeBytes("{".getBytes(getCharacterSet()));

        //ID
        if (message.getId() != null) {
            buf.writeBytes(ID);
            buf.writeBytes(QUOTE);
            buf.writeBytes(message.getId().toString().getBytes(getCharacterSet()));
            buf.writeBytes(QUOTE);
            buf.writeBytes(COMMA);
        }

        //EVENT
        buf.writeBytes(EVENT);
        buf.writeBytes(QUOTE);
        buf.writeBytes(message.getEvent().getName().getBytes(getCharacterSet()));
        buf.writeBytes(QUOTE);
        buf.writeBytes("}".getBytes(getCharacterSet()));


        //APP
        if (message.getAppId() != null) {
            buf.writeBytes(COMMA);
            buf.writeBytes(APPID);
            buf.writeBytes(QUOTE);
            buf.writeBytes(message.getAppId().getBytes(getCharacterSet()));
            buf.writeBytes(QUOTE);
        }

        //DATA
        if (message.getData() != null) {
            buf.writeBytes(COMMA);
            buf.writeBytes(DATA);
            buf.writeBytes(QUOTE);
            buf.writeBytes(message.getData().getBytes(getCharacterSet()));
            buf.writeBytes(QUOTE);
        }

        buf.writeBytes("}".getBytes(getCharacterSet()));
        TextWebSocketFrame frame = new TextWebSocketFrame(buf);
        logger.debug("Sending frame with data: " + frame.text());
        return frame;
    }
}
