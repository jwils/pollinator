package com.clover.push.message;

/**
 * User: josh
 * Date: 1/26/14
 */
public class AckMessage {
    private final Long id;
    private final String event;

    public AckMessage(Long id) {
        this.id = id;
        this.event = null;
    }

    public AckMessage(String event) {
        this.event = event;
        this.id = null;
    }


    public Long getId() {
        return id;
    }

    public String getEvent() {
        return event;
    }
}
