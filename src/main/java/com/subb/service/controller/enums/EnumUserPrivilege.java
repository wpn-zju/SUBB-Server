package com.subb.service.controller.enums;

public enum EnumUserPrivilege {
    PRIVILEGE_NORMAL("privilege_normal"),
    PRIVILEGE_ADMIN("privilege_admin"),
    PRIVILEGE_SUSPENDED("privilege_suspended");

    private final String token;

    EnumUserPrivilege(String token) { this.token = token; }

    @Override
    public String toString() {
        return token;
    }
}
