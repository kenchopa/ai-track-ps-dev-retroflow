package com.stackcraft.retroflow.exception;

public class ResourceNotFoundException extends RetroflowException {

    public ResourceNotFoundException(String entityType, Long id) {
        super(entityType + " " + id + ": not found");
    }
}
