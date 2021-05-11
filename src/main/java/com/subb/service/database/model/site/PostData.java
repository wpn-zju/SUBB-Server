package com.subb.service.database.model.site;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.subb.service.controller.enums.EnumPostStatus;
import com.subb.service.controller.utilities.EntityConstant;
import com.subb.service.database.InstantSerializer;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class PostData {
    @JsonProperty(EntityConstant.POST_DATA_POST_ID)
    private final int postId;
    @JsonProperty(EntityConstant.POST_DATA_THREAD_ID)
    private final int threadId;
    @JsonProperty(EntityConstant.POST_DATA_AUTHOR)
    private final int author;
    @JsonProperty(EntityConstant.POST_DATA_TIMESTAMP)
    @JsonSerialize(using = InstantSerializer.class)
    private final Instant timestamp;
    @JsonProperty(EntityConstant.POST_DATA_QUOTE_ID)
    private final int quoteId;
    @JsonProperty(EntityConstant.POST_DATA_CONTENT)
    private final String content;
    @JsonProperty(EntityConstant.POST_DATA_STATUS)
    private final EnumPostStatus status;
    @JsonProperty(EntityConstant.POST_DATA_COMMENTS)
    private final int comments;
    @JsonProperty(EntityConstant.POST_DATA_VOTES)
    private final int votes;
}
