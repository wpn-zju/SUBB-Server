package com.subb.service.database.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.subb.service.controller.utilities.EntityConstant;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public final class FileData {
    @JsonProperty(EntityConstant.FILE_DATA_FILE_ID)
    private final int fileId;
    @JsonProperty(EntityConstant.FILE_DATA_NAME)
    private final String name;
    @JsonProperty(EntityConstant.FILE_DATA_LINK)
    private final String link;
    @JsonProperty(EntityConstant.FILE_DATA_UPLOADER)
    private final int uploader;
    @JsonProperty(EntityConstant.FILE_DATA_TIMESTAMP)
    private final Instant timestamp;
    @JsonProperty(EntityConstant.FILE_DATA_SIZE)
    private final int size;
    @JsonProperty(EntityConstant.FILE_DATA_DOWNLOADS)
    private final int downloads;
}
