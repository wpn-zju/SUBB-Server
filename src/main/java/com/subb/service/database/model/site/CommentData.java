package com.subb.service.database.model.site;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.subb.service.controller.enums.EnumCommentStatus;
import com.subb.service.controller.utilities.EntityConstant;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class CommentData {
    @JsonProperty(EntityConstant.COMMENT_DATA_COMMENT_ID)
    private final int commentId;
    @JsonProperty(EntityConstant.COMMENT_DATA_POST_ID)
    private final int postId;
    @JsonProperty(EntityConstant.COMMENT_DATA_ROOT_ID)
    private final int rootId;
    @JsonProperty(EntityConstant.COMMENT_DATA_AUTHOR)
    private final int author;
    @JsonProperty(EntityConstant.COMMENT_DATA_TIMESTAMP)
    private final Instant timestamp;
    @JsonProperty(EntityConstant.COMMENT_DATA_QUOTE_ID)
    private final int quoteId;
    @JsonProperty(EntityConstant.COMMENT_DATA_CONTENT)
    private final String content;
    @JsonProperty(EntityConstant.COMMENT_DATA_STATUS)
    private final EnumCommentStatus status;
    @JsonProperty(EntityConstant.COMMENT_DATA_COMMENTS)
    private final int comments;
    @JsonProperty(EntityConstant.COMMENT_DATA_VOTES)
    private final int votes;
}
