package com.subb.service.controller.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EnumNotificationStatus {
    NOTIFICATION_STATUS_UNREAD("notification_status_unread"),
    NOTIFICATION_STATUS_READ("notification_status_read");

    private final String token;

    EnumNotificationStatus(String token) { this.token = token; }

    @JsonValue
    @Override
    public String toString() {
        return token;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EnumNotificationStatus fromString(String value) {
        for(EnumNotificationStatus v : values())
            if(v.token.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
