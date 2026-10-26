package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.CreateRetrospectiveRequest;
import com.stackcraft.retroflow.dto.RetrospectiveResponse;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.service.RetrospectiveService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api")
public class RetrospectiveController {

    private final RetrospectiveService retrospectiveService;

    public RetrospectiveController(RetrospectiveService retrospectiveService) {
        this.retrospectiveService = retrospectiveService;
    }

    /** 201 Created, 400 invalid input, 404 unknown team, 409 team already has an OPEN retrospective. */
    @PostMapping("/teams/{teamId}/retrospectives")
    public ResponseEntity<RetrospectiveResponse> createRetrospective(@PathVariable Long teamId,
                                                                     @Valid @RequestBody CreateRetrospectiveRequest request) {
        Retrospective retrospective = retrospectiveService.createRetrospective(teamId, request.title(), request.date());
        return ResponseEntity.created(URI.create("/api/retrospectives/" + retrospective.getId()))
                .body(RetrospectiveResponse.from(retrospective));
    }

    /** 200 OK, 404 unknown team. */
    @GetMapping("/teams/{teamId}/retrospectives")
    public List<RetrospectiveResponse> getRetrospectivesForTeam(@PathVariable Long teamId) {
        return retrospectiveService.getRetrospectivesForTeam(teamId).stream()
                .map(RetrospectiveResponse::from)
                .toList();
    }

    /** 200 OK, 404 unknown retrospective. */
    @GetMapping("/retrospectives/{id}")
    public RetrospectiveResponse getRetrospective(@PathVariable Long id) {
        return RetrospectiveResponse.from(retrospectiveService.getRetrospective(id));
    }

    /** 200 OK, 404 unknown retrospective, 409 already closed. */
    @PutMapping("/retrospectives/{id}/close")
    public RetrospectiveResponse closeRetrospective(@PathVariable Long id) {
        return RetrospectiveResponse.from(retrospectiveService.closeRetrospective(id));
    }

}
