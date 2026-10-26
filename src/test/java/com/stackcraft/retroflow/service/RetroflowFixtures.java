package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.Team;

import java.util.List;
import java.util.UUID;

/**
 * Builds the test data the service tests share. Team names get a random
 * suffix because the vendor tests commit their teams to the same database.
 */
final class RetroflowFixtures {

    static final String ALICE = "Alice";
    static final String BOB = "Bob";
    static final List<String> TWO_MEMBERS = List.of(ALICE, BOB);
    static final Long UNKNOWN_ID = 999_999L;
    static final String RETRO_TITLE = "Sprint 1 Retro";
    static final String WENT_WELL = "WENT_WELL";
    static final String NEEDS_IMPROVEMENT = "NEEDS_IMPROVEMENT";
    static final String HIGH = "HIGH";
    static final String LOW = "LOW";

    private final TeamService teamService;
    private final RetrospectiveService retrospectiveService;

    RetroflowFixtures(TeamService teamService, RetrospectiveService retrospectiveService) {
        this.teamService = teamService;
        this.retrospectiveService = retrospectiveService;
    }

    static String uniqueTeamName() {
        return "Team " + UUID.randomUUID();
    }

    Team team() {
        return teamService.createTeam(uniqueTeamName(), TWO_MEMBERS);
    }

    Retrospective openRetro(Team team) {
        return retrospectiveService.createRetrospective(team.getId(), RETRO_TITLE);
    }

    Retrospective openRetro() {
        return openRetro(team());
    }

    Retrospective closedRetro() {
        Retrospective retrospective = openRetro();
        retrospectiveService.closeRetrospective(retrospective.getId());
        return retrospective;
    }

    ActionItem actionItem(Retrospective retrospective, String priority) {
        return retrospectiveService.addActionItem(retrospective.getId(), "Fix the build", priority, ALICE);
    }

    /** An action item whose retrospective was closed after the item was added. */
    ActionItem actionItemInClosedRetro() {
        Retrospective retrospective = openRetro();
        ActionItem item = actionItem(retrospective, HIGH);
        retrospectiveService.closeRetrospective(retrospective.getId());
        return item;
    }

}
