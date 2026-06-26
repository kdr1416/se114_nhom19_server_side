package com.example.cafe_manager_api.exception;

import com.example.cafe_manager_api.dto.ConflictResponse;

public class ConflictException extends RuntimeException {
    private final ConflictResponse conflictResponse;

    public ConflictException(String message) {
        super(message);
        this.conflictResponse = new ConflictResponse(message, null);
    }

    public ConflictException(String message, java.util.List<ConflictResponse.ConflictingShiftDTO> conflicts) {
        super(message);
        this.conflictResponse = new ConflictResponse(message, conflicts);
    }

    public ConflictResponse getConflictResponse() {
        return conflictResponse;
    }
}
