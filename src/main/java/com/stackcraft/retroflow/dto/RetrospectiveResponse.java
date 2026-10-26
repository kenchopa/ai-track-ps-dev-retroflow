package com.stackcraft.retroflow.dto;

import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;

import java.time.LocalDate;

public record RetrospectiveResponse(
    Long id,
    Long teamId,
    String title,
    LocalDate date,
    RetrospectiveStatus status
) {
    public static RetrospectiveResponse from(Retrospective retrospective) {
        return new RetrospectiveResponse(retrospective.getId(), retrospective.getTeam().getId(),
            retrospective.getTitle(), retrospective.getDate(), retrospective.getStatus());
    }
}
