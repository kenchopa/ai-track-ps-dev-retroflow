package com.stackcraft.retroflow.dto;

import com.stackcraft.retroflow.entity.Team;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Body of {@code POST /api/teams}. Uniqueness of the team name and of member
 * names is checked by the service (decisions.md T1, T3).
 */
public record CreateTeamRequest(
        @NotBlank @Size(max = Team.MAX_NAME_LENGTH) String name,
        @NotEmpty List<@NotBlank @Size(max = Team.MAX_NAME_LENGTH) String> members) {
}
