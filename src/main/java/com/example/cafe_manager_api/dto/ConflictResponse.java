package com.example.cafe_manager_api.dto;

import java.util.List;

public class ConflictResponse {
    private String message;
    private List<ConflictingShiftDTO> conflictingShifts;

    public ConflictResponse() {}

    public ConflictResponse(String message, List<ConflictingShiftDTO> conflictingShifts) {
        this.message = message;
        this.conflictingShifts = conflictingShifts;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ConflictingShiftDTO> getConflictingShifts() {
        return conflictingShifts;
    }

    public void setConflictingShifts(List<ConflictingShiftDTO> conflictingShifts) {
        this.conflictingShifts = conflictingShifts;
    }

    public static class ConflictingShiftDTO {
        private Integer shiftId;
        private Long shiftDate;
        private String templateName;
        private String status;

        public ConflictingShiftDTO() {}

        public ConflictingShiftDTO(Integer shiftId, Long shiftDate, String templateName, String status) {
            this.shiftId = shiftId;
            this.shiftDate = shiftDate;
            this.templateName = templateName;
            this.status = status;
        }

        public Integer getShiftId() {
            return shiftId;
        }

        public void setShiftId(Integer shiftId) {
            this.shiftId = shiftId;
        }

        public Long getShiftDate() {
            return shiftDate;
        }

        public void setShiftDate(Long shiftDate) {
            this.shiftDate = shiftDate;
        }

        public String getTemplateName() {
            return templateName;
        }

        public void setTemplateName(String templateName) {
            this.templateName = templateName;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }
    }
}
