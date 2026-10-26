package com.stackcraft.retroflow.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateTeamRequest(
    @NotBlank @Size(max = 200) String name,
    @NotEmpty List<@NotBlank @Size(max = 200) String> members
) {
}
