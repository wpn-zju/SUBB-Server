package com.subb.service.database.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.subb.service.controller.enums.EnumMessageStatus;
import com.subb.service.controller.enums.EnumMessageType;
import com.subb.service.database.InstantSerializer;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class PrivateMessage {
    @JsonProperty("message_id")
    private final int messageId;
    @JsonProperty("message_sender")
    private final int messageSender;
    @JsonProperty("message_receiver")
    private final int messageReceiver;
    @JsonProperty("message_type")
    private final EnumMessageType messageType;
    @JsonProperty("message_status")
    private final EnumMessageStatus messageStatus;
    @JsonProperty("message_content")
    private final String messageContent;
    @JsonProperty("message_timestamp")
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant messageTimestamp;
}
