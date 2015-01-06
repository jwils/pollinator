package com.clover.push.message.encoder;

import com.clover.push.message.PushMessage;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultHttpContent;

import java.io.IOException;

/**
 * User: josh
 * Date: 1/29/14
 */
public class SSEPushEncoder extends PushMessageEncoderHandler {
    private static final byte[] LFCR = "\r\n".getBytes(getCharacterSet());
    private static final byte[] ID = "id: ".getBytes(getCharacterSet());
    private static final byte[] EVENT = "event: ".getBytes(getCharacterSet());
    private static final byte[] DATA = "data: ".getBytes(getCharacterSet());
    private static final byte[] APPID = "appId: ".getBytes(getCharacterSet());

    @Override
    public Object encode(ChannelHandlerContext ctx, PushMessage message) throws IOException {
        ByteBuf buf = ctx.alloc().buffer();
        if (message.getId() != null) {
            buf.writeBytes(ID).writeByte(message.getId().byteValue()).writeBytes(LFCR);
        }

        if (message.getEvent() != null) {
            buf.writeBytes(EVENT);
            buf.writeBytes(message.getEvent().getName().getBytes(getCharacterSet()));
            buf.writeBytes(LFCR);
        }

        if (message.getData() != null) {
            buf.writeBytes(DATA);
            buf.writeBytes(message.getData().getBytes(getCharacterSet()));
            buf.writeBytes(LFCR);
        }
        buf.writeBytes(LFCR);
        return new DefaultHttpContent(buf);
    }
}
