package com.subb.service.database.model.site;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.subb.service.controller.enums.EnumThreadStatus;
import com.subb.service.controller.utilities.EntityConstant;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class ThreadData {
    @JsonProperty(EntityConstant.THREAD_DATA_THREAD_ID)
    private final int threadId;
    @JsonProperty(EntityConstant.THREAD_DATA_FORUM_ID)
    private final int forumId;
    @JsonProperty(EntityConstant.THREAD_DATA_TITLE)
    private final String title;
    @JsonProperty(EntityConstant.THREAD_DATA_AUTHOR)
    private final int author;
    @JsonProperty(EntityConstant.THREAD_DATA_CREATE_TIMESTAMP)
    private final Instant createTimestamp;
    @JsonProperty(EntityConstant.THREAD_DATA_ACTIVE_TIMESTAMP)
    private final Instant activeTimestamp;
    @JsonProperty(EntityConstant.THREAD_DATA_STATUS)
    private final EnumThreadStatus status;
    @JsonProperty(EntityConstant.THREAD_DATA_POSTS)
    private final int posts;
    @JsonProperty(EntityConstant.THREAD_DATA_VIEWS)
    private final int views;
    @JsonProperty(EntityConstant.THREAD_DATA_VOTES)
    private final int votes;
    @JsonProperty(EntityConstant.THREAD_DATA_HEAT)
    private final int heat;
}
