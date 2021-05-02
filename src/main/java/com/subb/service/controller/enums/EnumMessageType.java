package com.subb.service.controller.enums;

public enum EnumMessageType {
    MESSAGE_TYPE_PRIVATE("message_private"),
    MESSAGE_TYPE_SYSTEM("message_system");

    private final String token;

    EnumMessageType(String token) { this.token = token; }

    @Override
    public String toString() {
        return token;
    }
}
