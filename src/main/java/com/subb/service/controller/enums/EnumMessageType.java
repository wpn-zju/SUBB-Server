package com.subb.service.controller.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EnumMessageType {
    MESSAGE_TYPE_SYSTEM("message_type_system"),
    MESSAGE_TYPE_PRIVATE("message_type_private");

    private final String token;

    EnumMessageType(String token) { this.token = token; }

    @JsonValue
    @Override
    public String toString() {
        return token;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EnumMessageType fromString(String value) {
        for(EnumMessageType v : values())
            if(v.token.equalsIgnoreCase(value)) return v;
        throw new IllegalArgumentException();
    }
}
