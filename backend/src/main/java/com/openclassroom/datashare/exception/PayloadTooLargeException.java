package com.openclassroom.datashare.exception;

/** 413: uploaded file exceeds the maximum allowed size. */
public class PayloadTooLargeException extends RuntimeException {
    public PayloadTooLargeException(String message) {
        super(message);
    }
}
