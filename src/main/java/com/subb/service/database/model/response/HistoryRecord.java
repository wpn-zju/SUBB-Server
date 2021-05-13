package com.subb.service.database.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.subb.service.database.InstantSerializer;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class HistoryRecord {
    @JsonProperty("history_record_id")
    private final int historyRecordId;
    @JsonProperty("history_user_id")
    private final int historyUserId;
    @JsonProperty("history_thread_id")
    private final int historyThreadId;
    @JsonProperty("history_timestamp")
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant historyTimestamp;
}
