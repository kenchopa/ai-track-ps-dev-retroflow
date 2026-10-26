package com.stackcraft.retroflow.exception;

/**
 * Thrown when a requested resource does not exist. Mapped to 404.
 */
public class NotFoundException extends RetroflowException {

    public NotFoundException(String entityType, Long id) {
        super(entityType + " " + id + ": not found");
    }

}
