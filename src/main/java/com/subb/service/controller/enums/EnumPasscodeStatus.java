package com.subb.service.controller.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EnumPasscodeStatus {
    PASSCODE_STATUS_VALID("passcode_status_valid"),
    PASSCODE_STATUS_EXPIRED("passcode_status_expired"),
    PASSCODE_STATUS_REVOKED("passcode_status_revoked");

    private final String token;

    EnumPasscodeStatus(String token) { this.token = token; }

    @JsonValue
    @Override
    public String toString() {
        return token;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EnumPasscodeStatus fromString(String value) {
        for(EnumPasscodeStatus v : values())
            if(v.token.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
