package com.subb.service.database.model.site;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.subb.service.controller.utilities.EntityConstant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ForumData {
    @JsonProperty(EntityConstant.FORUM_DATA_FORUM_ID)
    private final int forumId;
    @JsonProperty(EntityConstant.FORUM_DATA_TITLE)
    private final String title;
    @JsonProperty(EntityConstant.FORUM_DATA_THREADS)
    private final int threads;
    @JsonProperty(EntityConstant.FORUM_DATA_HEAT)
    private final int heat;
}
