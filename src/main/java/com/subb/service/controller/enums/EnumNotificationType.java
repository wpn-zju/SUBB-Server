package com.subb.service.controller.enums;

public enum EnumNotificationType {
    ENUM_NOTIFICATION_TYPE_COMMENT_QUOTE("notification_type_comment_quote"),
    ENUM_NOTIFICATION_TYPE_COMMENT_ROOT_AUTHOR("notification_type_comment_root_author"),
    ENUM_NOTIFICATION_TYPE_POST_AUTHOR("notification_type_post_author"),
    ENUM_NOTIFICATION_TYPE_POST_QUOTE("notification_type_post_quote"),
    ENUM_NOTIFICATION_TYPE_THREAD_AUTHOR("notification_type_thread_author");

    private final String token;

    EnumNotificationType(String token) { this.token = token; }

    @Override
    public String toString() {
        return token;
    }
}
