package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.CreateTeamRequest;
import com.stackcraft.retroflow.dto.TeamResponse;
import com.stackcraft.retroflow.service.TeamService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        var team = teamService.createTeam(request.name(), request.members());
        var location = ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}").buildAndExpand(team.getId()).toUri();
        return ResponseEntity.created(location).body(TeamResponse.from(team));
    }

    @GetMapping("/{id}")
    public TeamResponse getTeam(@PathVariable @Positive(message = "must be greater than zero") Long id) {
        return TeamResponse.from(teamService.getTeam(id));
    }
}
