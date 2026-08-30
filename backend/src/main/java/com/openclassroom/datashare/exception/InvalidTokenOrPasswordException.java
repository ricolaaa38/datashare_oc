package com.openclassroom.datashare.exception;

/** 401: invalid download token or invalid file password. */
public class InvalidTokenOrPasswordException extends RuntimeException {
    public InvalidTokenOrPasswordException(String message) {
        super(message);
    }
}
