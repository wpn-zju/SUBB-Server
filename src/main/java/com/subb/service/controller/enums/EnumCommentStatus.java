package com.subb.service.controller.enums;

public enum EnumCommentStatus {
    COMMENT_STATUS_DRAFT("comment_status_draft"),
    COMMENT_STATUS_UNDER_REVIEW("comment_status_under_review"),
    COMMENT_STATUS_VISIBLE("comment_status_visible"),
    COMMENT_STATUS_DELETED("comment_status_deleted");

    private final String token;

    EnumCommentStatus(String token) { this.token = token; }

    @Override
    public String toString() {
        return token;
    }
}
