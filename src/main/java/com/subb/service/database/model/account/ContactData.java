package com.subb.service.database.model.account;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.subb.service.controller.enums.EnumGender;
import com.subb.service.controller.enums.EnumUserPrivilege;
import com.subb.service.controller.utilities.EntityConstant;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class ContactData {
    @JsonProperty(EntityConstant.CONTACT_DATA_CONTACT_ID)
    private final int contactId;
    @JsonProperty(EntityConstant.CONTACT_DATA_EMAIL)
    private final String email;
    @JsonProperty(EntityConstant.CONTACT_DATA_NICKNAME)
    private final String nickname;
    @JsonProperty(EntityConstant.CONTACT_DATA_PRIVILEGE)
    private final EnumUserPrivilege privilege;
    @JsonProperty(EntityConstant.CONTACT_DATA_GENDER)
    private final EnumGender gender;
    @JsonProperty(EntityConstant.CONTACT_DATA_AVATAR_LINK)
    private final String avatarLink;
    @JsonProperty(EntityConstant.CONTACT_DATA_PERSONAL_INFO)
    private final String personalInfo;
    @JsonProperty(EntityConstant.CONTACT_DATA_POSTS)
    private final int posts;
    @JsonProperty(EntityConstant.CONTACT_DATA_EXP)
    private final int exp;
    @JsonProperty(EntityConstant.CONTACT_DATA_PRESTIGE)
    private final int prestige;
}
