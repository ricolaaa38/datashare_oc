package com.openclassroom.datashare.exception;

/** 410: the file's expiration date has been reached. */
public class FileExpiredException extends RuntimeException {
    public FileExpiredException(String message) {
        super(message);
    }
}
