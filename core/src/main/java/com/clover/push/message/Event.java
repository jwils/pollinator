package com.clover.push.message;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * User: josh
 * Date: 1/6/14
 */
public class Event {
    private final String eventName;
    private final boolean shouldGroup;
    private final boolean persistent;

    public Event(@JsonProperty("name") String eventName) {
        this(eventName, false, true);
    }

    public Event(String eventName, boolean shouldGroup) {
        this(eventName, shouldGroup, true);
    }

    private Event(String eventName, boolean shouldGroup, boolean persistent) {
        if (!persistent && !shouldGroup) {
            throw new IllegalStateException("Persistent messages must be grouped.");
        }
        this.eventName = eventName;
        this.shouldGroup = shouldGroup;
        this.persistent = persistent;
    }

    public Event newNonPersistentEvent(String name) {
        return new Event(name, true, true);
    }

    public String getName() {
        return eventName;
    }

    public boolean shouldGroup() {
        return shouldGroup;
    }

    public boolean isPersistent() {
        return persistent;
    }

    @Override
    public String toString() {
        return eventName;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof Event && this.eventName.equals(((Event) other).eventName);
    }

    @Override
    public int hashCode() {
        return eventName != null ? eventName.hashCode() : 0;
    }
}
