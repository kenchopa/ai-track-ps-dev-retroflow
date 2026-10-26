package com.stackcraft.retroflow.dto;

import java.time.Instant;

public record ApiError(String error, int status, Instant timestamp) {
}
