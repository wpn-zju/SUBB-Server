package com.subb.service.controller.enums;

public enum EnumPasscodeStatus {
    PASSCODE_STATUS_VALID("passcode_status_valid"),
    PASSCODE_STATUS_EXPIRED("passcode_status_expired"),
    PASSCODE_STATUS_REVOKED("passcode_status_revoked");

    private final String token;

    EnumPasscodeStatus(String token) { this.token = token; }

    @Override
    public String toString() {
        return token;
    }
}
