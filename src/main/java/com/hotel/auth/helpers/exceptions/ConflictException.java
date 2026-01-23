package com.hotel.auth.helpers.exceptions;

public class ConflictException extends RuntimeException {

    private final String conflictType;

    public ConflictException(String message, String conflictType) {
        super(message);
        this.conflictType = conflictType;
    }

    public String getConflictType() {
        return conflictType;
    }
}
