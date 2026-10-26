package com.stackcraft.retroflow.dto;

import com.stackcraft.retroflow.entity.Retrospective;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Body of {@code POST /api/teams/{teamId}/retrospectives}.
 *
 * @param title required
 * @param date  optional, ISO-8601 ({@code 2026-10-26}); defaults to today (decisions.md R1)
 */
public record CreateRetrospectiveRequest(
        @NotBlank @Size(max = Retrospective.MAX_TITLE_LENGTH) String title,
        LocalDate date) {
}
