package com.stackcraft.retroflow.exception;

/**
 * Thrown by the service layer for input that is well-formed but invalid in
 * context, e.g. an author who is not a member of the team. Mapped to 400.
 */
public class InvalidInputException extends RetroflowException {

    public InvalidInputException(String message) {
        super(message);
    }

}
