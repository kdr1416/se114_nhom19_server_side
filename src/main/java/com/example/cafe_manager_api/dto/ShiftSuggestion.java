package com.example.cafe_manager_api.dto;

import java.util.List;

public class ShiftSuggestion {
    private Integer shiftId;
    private Long shiftDate;
    private Integer templateId;
    private String templateName;
    private String startTime;       // "HH:mm"
    private String endTime;         // "HH:mm"
    private Integer minStaff;
    private List<Integer> suggestedUserIds;
    private List<String> suggestedUserNames;
    private Boolean isFulfilled;
    private Integer missingCount;

    public ShiftSuggestion() {}

    public ShiftSuggestion(Integer shiftId, Long shiftDate, Integer templateId, String templateName,
                          String startTime, String endTime, Integer minStaff,
                          List<Integer> suggestedUserIds, List<String> suggestedUserNames,
                          Boolean isFulfilled, Integer missingCount) {
        this.shiftId = shiftId;
        this.shiftDate = shiftDate;
        this.templateId = templateId;
        this.templateName = templateName;
        this.startTime = startTime;
        this.endTime = endTime;
        this.minStaff = minStaff;
        this.suggestedUserIds = suggestedUserIds;
        this.suggestedUserNames = suggestedUserNames;
        this.isFulfilled = isFulfilled;
        this.missingCount = missingCount;
    }

    public Integer getShiftId() { return shiftId; }
    public void setShiftId(Integer shiftId) { this.shiftId = shiftId; }
    public Long getShiftDate() { return shiftDate; }
    public void setShiftDate(Long shiftDate) { this.shiftDate = shiftDate; }
    public Integer getTemplateId() { return templateId; }
    public void setTemplateId(Integer templateId) { this.templateId = templateId; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
    public Integer getMinStaff() { return minStaff; }
    public void setMinStaff(Integer minStaff) { this.minStaff = minStaff; }
    public List<Integer> getSuggestedUserIds() { return suggestedUserIds; }
    public void setSuggestedUserIds(List<Integer> suggestedUserIds) { this.suggestedUserIds = suggestedUserIds; }
    public List<String> getSuggestedUserNames() { return suggestedUserNames; }
    public void setSuggestedUserNames(List<String> suggestedUserNames) { this.suggestedUserNames = suggestedUserNames; }
    public Boolean getIsFulfilled() { return isFulfilled; }
    public void setIsFulfilled(Boolean isFulfilled) { this.isFulfilled = isFulfilled; }
    public Integer getMissingCount() { return missingCount; }
    public void setMissingCount(Integer missingCount) { this.missingCount = missingCount; }
}
