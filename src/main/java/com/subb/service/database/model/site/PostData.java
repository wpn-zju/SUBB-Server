package com.subb.service.database.model.site;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.subb.service.controller.enums.EnumPostStatus;
import com.subb.service.controller.utilities.EntityConstant;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class PostData {
    @JsonProperty(EntityConstant.POST_INFO_POST_ID)
    private final int postId;
    @JsonProperty(EntityConstant.POST_INFO_THREAD_ID)
    private final int threadId;
    @JsonProperty(EntityConstant.POST_INFO_AUTHOR)
    private final int author;
    @JsonProperty(EntityConstant.POST_INFO_TIMESTAMP)
    private final Instant timestamp;
    @JsonProperty(EntityConstant.POST_INFO_REFER_TO)
    private final int referTo;
    @JsonProperty(EntityConstant.POST_INFO_CONTENT)
    private final String content;
    @JsonProperty(EntityConstant.POST_INFO_STATUS)
    private final EnumPostStatus status;
    @JsonProperty(EntityConstant.POST_INFO_COMMENTS)
    private final int comments;
    @JsonProperty(EntityConstant.POST_INFO_VOTES)
    private final int votes;
}
