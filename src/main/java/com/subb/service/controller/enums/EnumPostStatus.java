package com.subb.service.controller.enums;

public enum EnumPostStatus {
    POST_STATUS_DRAFT("post_status_draft"),
    POST_STATUS_VISIBLE("post_status_visible"),
    POST_STATUS_DELETED("post_status_deleted");

    private final String token;

    EnumPostStatus(String token) { this.token = token; }

    @Override
    public String toString() {
        return token;
    }
}
