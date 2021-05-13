package com.subb.service.controller.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum EnumGender {
    GENDER_HIDDEN("gender_hidden"),
    GENDER_MALE("gender_male"),
    GENDER_FEMALE("gender_female"),
    GENDER_OTHERS("gender_others");

    private final String token;

    EnumGender(String token) { this.token = token; }

    @JsonValue
    @Override
    public String toString() {
        return token;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static EnumGender fromString(String value) {
        for(EnumGender v : values())
            if(v.token.equalsIgnoreCase(value)) return v;
        return GENDER_HIDDEN;
    }
}
