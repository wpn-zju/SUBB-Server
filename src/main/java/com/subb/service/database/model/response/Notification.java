package com.subb.service.database.model.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.subb.service.controller.enums.EnumNotificationStatus;
import com.subb.service.controller.enums.EnumNotificationType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class Notification {
    @JsonProperty("notification_id")
    private final int notificationId;
    @JsonProperty("notification_user_id")
    private final int notificationUserId;
    @JsonProperty("notification_reply_id")
    private final int notificationReplyId;
    @JsonProperty("notification_type")
    private final EnumNotificationType notificationType;
    @JsonProperty("notification_status")
    private final EnumNotificationStatus notificationStatus;
}
