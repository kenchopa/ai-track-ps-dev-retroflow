package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.CreateRetrospectiveRequest;
import com.stackcraft.retroflow.dto.RetrospectiveResponse;
import com.stackcraft.retroflow.service.RetrospectiveService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api")
public class RetrospectiveController {

    private final RetrospectiveService retrospectiveService;

    public RetrospectiveController(RetrospectiveService retrospectiveService) {
        this.retrospectiveService = retrospectiveService;
    }

    @PostMapping("/teams/{teamId}/retrospectives")
    public ResponseEntity<RetrospectiveResponse> createRetrospective(
            @PathVariable @Positive(message = "must be greater than zero") Long teamId,
            @Valid @RequestBody CreateRetrospectiveRequest request) {
        var retro = retrospectiveService.createRetrospective(teamId, request.title(), request.date());
        var location = ServletUriComponentsBuilder.fromCurrentContextPath()
            .path("/api/retrospectives/{id}").buildAndExpand(retro.getId()).toUri();
        return ResponseEntity.created(location).body(RetrospectiveResponse.from(retro));
    }

    @GetMapping("/teams/{teamId}/retrospectives")
    public List<RetrospectiveResponse> listRetrospectives(
            @PathVariable @Positive(message = "must be greater than zero") Long teamId) {
        return retrospectiveService.listRetrospectives(teamId).stream()
            .map(RetrospectiveResponse::from).toList();
    }

    @PutMapping("/retrospectives/{id}/close")
    public ResponseEntity<Void> closeRetrospective(
            @PathVariable @Positive(message = "must be greater than zero") Long id) {
        retrospectiveService.closeRetrospective(id);
        return ResponseEntity.noContent().build();
    }
}
