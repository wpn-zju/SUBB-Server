package com.subb.service.controller.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EnumNotificationType {
    ENUM_NOTIFICATION_TYPE_COMMENT_QUOTE("notification_type_comment_quote"),
    ENUM_NOTIFICATION_TYPE_COMMENT_ROOT_AUTHOR("notification_type_root_author"),
    ENUM_NOTIFICATION_TYPE_POST_AUTHOR("notification_type_post_author"),
    ENUM_NOTIFICATION_TYPE_POST_QUOTE("notification_type_post_quote"),
    ENUM_NOTIFICATION_TYPE_THREAD_AUTHOR("notification_type_thread_author");

    private final String token;

    EnumNotificationType(String token) { this.token = token; }

    @JsonValue
    @Override
    public String toString() {
        return token;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EnumNotificationType fromString(String value) {
        for(EnumNotificationType v : values())
            if(v.token.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
