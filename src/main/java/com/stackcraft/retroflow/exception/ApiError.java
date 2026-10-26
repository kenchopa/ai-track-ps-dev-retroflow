package com.stackcraft.retroflow.exception;

import java.time.Instant;

/**
 * Error body returned for every failed request (StackCraft standard 2.2).
 *
 * @param error     message in the form {@code [EntityType] [identifier]: [reason]}
 * @param status    HTTP status code
 * @param timestamp when the error occurred
 */
public record ApiError(String error, int status, Instant timestamp) {
}
