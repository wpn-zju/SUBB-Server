package com.subb.service.controller.enums;

public enum EnumMessageStatus {
    MESSAGE_STATUS_UNREAD("message_status_unread"),
    MESSAGE_STATUS_READ("message_status_read");

    private final String token;

    EnumMessageStatus(String token) { this.token = token; }

    @Override
    public String toString() {
        return token;
    }
}
