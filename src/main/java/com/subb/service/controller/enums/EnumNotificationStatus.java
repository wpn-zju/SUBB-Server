package com.subb.service.controller.enums;

public enum EnumNotificationStatus {
    NOTIFICATION_STATUS_UNREAD("notification_status_unread"),
    NOTIFICATION_STATUS_READ("notification_status_read");

    private final String token;

    EnumNotificationStatus(String token) { this.token = token; }

    @Override
    public String toString() {
        return token;
    }
}
