package com.subb.service.controller.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EnumPostStatus {
    POST_STATUS_DRAFT("post_status_draft"),
    POST_STATUS_VISIBLE("post_status_visible"),
    POST_STATUS_DELETED("post_status_deleted");

    private final String token;

    EnumPostStatus(String token) { this.token = token; }

    @JsonValue
    @Override
    public String toString() {
        return token;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EnumPostStatus fromString(String value) {
        for(EnumPostStatus v : values())
            if(v.token.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
