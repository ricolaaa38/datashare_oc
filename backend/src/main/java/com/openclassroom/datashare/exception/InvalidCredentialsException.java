package com.openclassroom.datashare.exception;

/** 401: login does not exist or password does not match. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException(String message) {
        super(message);
    }
}
