package com.example.cafe_manager_api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class OverlapException extends RuntimeException {
    public OverlapException(String message) {
        super(message);
    }
}
