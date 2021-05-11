package com.subb.service.controller.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EnumSessionStatus {
    SESSION_STATUS_VALID("session_status_valid"),
    SESSION_STATUS_EXPIRED("session_status_expired"),
    SESSION_STATUS_REVOKED("session_status_revoked");

    private final String token;

    EnumSessionStatus(String token) { this.token = token; }

    @JsonValue
    @Override
    public String toString() {
        return token;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EnumSessionStatus fromString(String value) {
        for(EnumSessionStatus v : values())
            if(v.token.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
