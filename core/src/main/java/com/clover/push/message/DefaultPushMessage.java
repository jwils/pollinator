package com.clover.push.message;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * User: josh
 * Date: 1/3/14
 */
public class DefaultPushMessage implements PushMessage {
    private final Long id;

    private String data;
    private Event event;
    private String appId;

    public DefaultPushMessage(Event event, String data, String appId) {
        this(null, event, data, appId);
    }


    public DefaultPushMessage(@JsonProperty("id") Long id, @JsonProperty("event") Event event,
                              @JsonProperty("data") String data, @JsonProperty("appId") String appId) {
        this.id = id;
        this.event = event;
        this.data = data;
        this.appId = appId;
    }

    @Override
    public Long getId() {
        return id;
    }

    @Override
    public String getAppId() {
        return appId;
    }

    public void setAppId(String appId) {
        this.appId = appId;
    }

    @Override
    public Event getEvent() {
        return event;
    }

    public void setEvent(Event event) {
        this.event = event;
    }

    @Override
    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof PushMessage) {
            PushMessage otherMsg = ((PushMessage) other);
            if ((event == null && otherMsg.getEvent() != null) ||
                    (event != null && !event.equals(otherMsg.getEvent()))) {
                return false;
            }
            if ((data == null && otherMsg.getData() != null)
                    || (data != null && !data.equals(otherMsg.getData()))) {
                return false;
            }
            if ((appId == null && otherMsg.getAppId() != null)
                    || (appId != null && !appId.equals(otherMsg.getAppId()))) {
                return false;
            }
            return true;
        }
        return false;
    }

    @Override
    public int hashCode() {
        int hash = 0;
        if (event != null) {
            hash = hash ^ event.hashCode();
        }
        if (appId != null) {
            hash = hash ^ appId.hashCode();
        }
        if (data != null) {
            hash = hash ^ data.hashCode();
        }
        return hash;
    }
}
