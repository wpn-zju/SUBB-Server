package com.subb.service.controller.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public class SmallTalkResponseBody {
    @JsonProperty("code")
    private final int code;
    @JsonProperty("message")
    private final String message;
    @JsonProperty("data")
    private final Object data;

    public SmallTalkResponseBody(int returnCode, String message) {
        this.code = returnCode;
        this.message = message;
        this.data = null;
    }

    public SmallTalkResponseBody(int returnCode, String message, Object data) {
        this.code = returnCode;
        this.message = message;
        this.data = data;
    }
}
