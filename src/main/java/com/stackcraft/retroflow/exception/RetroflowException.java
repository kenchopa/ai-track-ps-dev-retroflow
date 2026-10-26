package com.stackcraft.retroflow.exception;

/**
 * Thrown when a request violates one of RetroFlow's business rules — e.g.
 * attempting to modify a closed retrospective, creating a second OPEN
 * retrospective for a team, or uncompleting an already-completed action
 * item.
 *
 * Mapped to HTTP 409 by the global exception handler. Resource-not-found
 * failures use the more specific ResourceNotFoundException and HTTP 404.
 */
public class RetroflowException extends RuntimeException {

    public RetroflowException(String message) {
        super(message);
    }

}
