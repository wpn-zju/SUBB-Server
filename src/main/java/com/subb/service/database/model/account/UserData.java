package com.subb.service.database.model.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.subb.service.controller.enums.EnumGender;
import com.subb.service.controller.enums.EnumUserPrivilege;
import com.subb.service.controller.utilities.EntityConstant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class UserData {
    @JsonProperty(EntityConstant.USER_DATA_USER_ID)
    private final int userId;
    @JsonProperty(EntityConstant.USER_DATA_EMAIL)
    private final String email;
    @JsonProperty(EntityConstant.USER_DATA_NICKNAME)
    private final String nickname;
    @JsonProperty(EntityConstant.USER_DATA_PASSWORD_HASH)
    private final String passwordHash;
    @JsonProperty(EntityConstant.USER_DATA_PRIVILEGE)
    private final EnumUserPrivilege privilege;
    @JsonProperty(EntityConstant.USER_DATA_GENDER)
    private final EnumGender gender;
    @JsonProperty(EntityConstant.USER_DATA_AVATAR_LINK)
    private final String avatarLink;
    @JsonProperty(EntityConstant.USER_DATA_PERSONAL_INFO)
    private final String personalInfo;
    @JsonProperty(EntityConstant.USER_DATA_POSTS)
    private final int posts;
    @JsonProperty(EntityConstant.USER_DATA_EXP)
    private final int exp;
    @JsonProperty(EntityConstant.USER_DATA_PRESTIGE)
    private final int prestige;
}
