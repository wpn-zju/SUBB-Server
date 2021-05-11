package com.subb.service.controller.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EnumUserPrivilege {
    PRIVILEGE_NORMAL("privilege_normal"),
    PRIVILEGE_ADMIN("privilege_admin"),
    PRIVILEGE_SUSPENDED("privilege_suspended");

    private final String token;

    EnumUserPrivilege(String token) { this.token = token; }

    @JsonValue
    @Override
    public String toString() {
        return token;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EnumUserPrivilege fromString(String value) {
        for(EnumUserPrivilege v : values())
            if(v.token.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
