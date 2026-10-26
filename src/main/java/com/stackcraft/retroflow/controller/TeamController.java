package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.CreateTeamRequest;
import com.stackcraft.retroflow.dto.TeamResponse;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.service.TeamService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    private final TeamService teamService;

    public TeamController(TeamService teamService) {
        this.teamService = teamService;
    }

    /** 201 Created, 400 invalid input, 409 duplicate team name. */
    @PostMapping
    public ResponseEntity<TeamResponse> createTeam(@Valid @RequestBody CreateTeamRequest request) {
        Team team = teamService.createTeam(request.name(), request.members());
        return ResponseEntity.created(URI.create("/api/teams/" + team.getId())).body(TeamResponse.from(team));
    }

    /** 200 OK. */
    @GetMapping
    public List<TeamResponse> getAllTeams() {
        return teamService.getAllTeams().stream().map(TeamResponse::from).toList();
    }

    /** 200 OK, 404 unknown team. */
    @GetMapping("/{id}")
    public TeamResponse getTeam(@PathVariable Long id) {
        return TeamResponse.from(teamService.getTeam(id));
    }

}
