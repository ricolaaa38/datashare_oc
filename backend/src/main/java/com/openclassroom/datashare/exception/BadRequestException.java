package com.openclassroom.datashare.exception;

/** 400: request input fails a business validation rule. */
public class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}
