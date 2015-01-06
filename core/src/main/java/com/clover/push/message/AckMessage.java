package com.clover.push.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * User: josh
 * Date: 1/26/14
 */
public class AckMessage extends DefaultPushMessage {
    public AckMessage( Long id) {
        super(id, null, null, null);
    }

    public AckMessage(Event event) {
        super(null, event, null, null);
    }

    //jackson
    public AckMessage(@JsonProperty("id") Long id, @JsonProperty("event") Event event) {
        super(id, event, null, null);
    }
}
