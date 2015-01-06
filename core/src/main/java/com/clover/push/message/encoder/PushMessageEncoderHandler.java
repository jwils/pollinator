package com.clover.push.message.encoder;

import com.clover.push.message.PushMessage;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToMessageEncoder;
import io.netty.util.CharsetUtil;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.List;


/**
 * User: josh
 * Date: 1/3/14
 */
public abstract class PushMessageEncoderHandler<T> extends MessageToMessageEncoder<PushMessage> {
    private static final Logger logger = LoggerFactory.getLogger(PushMessageEncoderHandler.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, PushMessage msg, List<Object> out) throws Exception {
        try {
            out.add(encode(ctx, msg));
        } catch (IOException e) {
            logger.error("Unable to encode PushMessage", e);
        }
    }

    public abstract T encode(ChannelHandlerContext ctx, PushMessage message) throws IOException;


    protected static Charset getCharacterSet() {
        return CharsetUtil.UTF_8;
    }
}
