package com.clover.push.server.service.redis;

import com.clover.push.message.PushMessage;

/**
 * User: josh
 * Date: 3/10/14
 */
class PushMessageHolder {
    public String clientKey;
    public PushMessage message;
    public int timeoutSec;
}
