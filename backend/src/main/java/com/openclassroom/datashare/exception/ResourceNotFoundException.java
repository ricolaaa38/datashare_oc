package com.openclassroom.datashare.exception;

/**
 * 404: the requested resource does not exist (or is not visible to the caller).
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}
