package com.example.cafe_manager_api.dto;

import jakarta.validation.constraints.NotNull;

public class PublishAvailabilityRequest {
    @NotNull(message = "templateId is required")
    private Integer templateId;

    @NotNull(message = "dayOfWeek is required")
    private Integer dayOfWeek;

    @NotNull(message = "isAvailable is required")
    private Boolean isAvailable;

    @NotNull(message = "scope is required")
    private String scope;  // "THIS_WEEK" or "UNTIL_DATE"

    private Long untilDate;  // required if scope = "UNTIL_DATE"

    public Integer getTemplateId() {
        return templateId;
    }

    public void setTemplateId(Integer templateId) {
        this.templateId = templateId;
    }

    public Integer getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(Integer dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public String getScope() {
        return scope;
    }

    public void setScope(String scope) {
        this.scope = scope;
    }

    public Long getUntilDate() {
        return untilDate;
    }

    public void setUntilDate(Long untilDate) {
        this.untilDate = untilDate;
    }
}
