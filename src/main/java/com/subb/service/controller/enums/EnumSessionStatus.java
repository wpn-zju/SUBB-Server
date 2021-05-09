package com.subb.service.controller.enums;

public enum EnumSessionStatus {
    SESSION_STATUS_VALID("session_status_valid"),
    SESSION_STATUS_EXPIRED("session_status_expired"),
    SESSION_STATUS_REVOKED("session_status_revoked");

    private final String token;

    EnumSessionStatus(String token) { this.token = token; }

    @Override
    public String toString() {
        return token;
    }
}
