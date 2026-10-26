package com.stackcraft.retroflow.dto;

import com.stackcraft.retroflow.entity.Team;

import java.util.List;

public record TeamResponse(Long id, String name, List<String> members) {

    public TeamResponse {
        members = List.copyOf(members);
    }

    public static TeamResponse from(Team team) {
        return new TeamResponse(team.getId(), team.getName(), team.getMembers());
    }
}
