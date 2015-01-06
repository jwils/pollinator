package com.clover.push.message;


import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * User: josh
 * Date: 1/6/14
 */
public class Event {
    public static final Event ITEM_SYNC = new Event("item_sync", true);
    public static final Event MASTER_CLEAR = new Event("master_clear", false);
    public static final Event REFRESH_MERCHANT_PROPS = new Event("refresh_merchant_props", true);
    public static final Event APPS = new Event("apps", true);
    public static final Event ROM_UPGRADE = new Event("rom_upgrade", true);
    public static final Event RELOAD_KEY = new Event("reloadkey", true);
    public static final Event SEND_DEBUG = new Event("send_debug", false);
    public static final Event REBOOT = new Event("reboot", false);
    public static final Event KEEP_ALIVE = new Event("keepalive", true);
    public static final Event FAILED_TRANSACTION = new Event("failed_transaction", false);
    public static final Event FIRE_ORDER = new Event("fire_order", false);

    private final String eventName;
    private final boolean shouldGroup;

    public Event(@JsonProperty("name") String eventName) {
        this.eventName = eventName;
        shouldGroup = false;
    }

    public Event(String eventName, boolean shouldGroup) {
        this.eventName = eventName;
        this.shouldGroup = shouldGroup;
    }

    public String getName() {
        return eventName;
    }

    public boolean shouldGroup() {
        return shouldGroup;
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
