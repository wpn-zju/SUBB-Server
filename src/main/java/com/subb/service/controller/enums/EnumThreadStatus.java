package com.subb.service.controller.enums;

public enum EnumThreadStatus {
    THREAD_STATUS_DRAFT("thread_status_draft"),
    THREAD_STATUS_VISIBLE("thread_status_visible"),
    THREAD_STATUS_DELETED("thread_status_deleted");

    private final String token;

    EnumThreadStatus(String token) { this.token = token; }

    @Override
    public String toString() {
        return token;
    }
}
