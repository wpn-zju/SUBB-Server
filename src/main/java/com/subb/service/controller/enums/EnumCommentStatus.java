package com.subb.service.controller.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EnumCommentStatus {
    COMMENT_STATUS_DRAFT("comment_status_draft"),
    COMMENT_STATUS_VISIBLE("comment_status_visible"),
    COMMENT_STATUS_DELETED("comment_status_deleted");

    private final String token;

    EnumCommentStatus(String token) { this.token = token; }

    @JsonValue
    @Override
    public String toString() {
        return token;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EnumCommentStatus fromString(String value) {
        for(EnumCommentStatus v : values())
            if(v.token.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
