package com.subb.service.controller.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EnumThreadStatus {
    THREAD_STATUS_DRAFT("thread_status_draft"),
    THREAD_STATUS_VISIBLE("thread_status_visible"),
    THREAD_STATUS_DELETED("thread_status_deleted");

    private final String token;

    EnumThreadStatus(String token) { this.token = token; }

    @JsonValue
    @Override
    public String toString() {
        return token;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EnumThreadStatus fromString(String value) {
        for(EnumThreadStatus v : values())
            if(v.token.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
