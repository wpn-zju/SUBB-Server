package com.subb.service.controller.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EnumMessageStatus {
    MESSAGE_STATUS_UNREAD("message_status_unread"),
    MESSAGE_STATUS_READ("message_status_read");

    private final String token;

    EnumMessageStatus(String token) { this.token = token; }

    @JsonValue
    @Override
    public String toString() {
        return token;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EnumMessageStatus fromString(String value) {
        for(EnumMessageStatus v : values())
            if(v.token.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
