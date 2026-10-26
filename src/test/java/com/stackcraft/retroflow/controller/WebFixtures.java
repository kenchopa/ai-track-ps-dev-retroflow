package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.Team;

import java.time.LocalDate;
import java.util.List;

/**
 * Entities with fixed ids, returned by the mocked services.
 */
final class WebFixtures {

    static final long TEAM_ID = 1L;
    static final long RETRO_ID = 10L;
    static final long FEEDBACK_ID = 100L;
    static final long ACTION_ID = 200L;
    static final long UNKNOWN_ID = 999L;
    static final String TEAM_NAME = "Team Alpha";
    static final String ALICE = "Alice";
    static final List<String> MEMBERS = List.of(ALICE, "Bob");
    static final String RETRO_TITLE = "Sprint 1 Retro";
    static final LocalDate RETRO_DATE = LocalDate.of(2026, 10, 26);
    static final String CONTENT = "Pairing worked well";
    static final String ACTION_CONTENT = "Fix the build";

    private WebFixtures() {
    }

    static Team team() {
        Team team = new Team(TEAM_NAME, MEMBERS);
        team.setId(TEAM_ID);
        return team;
    }

    static Retrospective retro() {
        Retrospective retrospective = new Retrospective(team(), RETRO_TITLE, RETRO_DATE);
        retrospective.setId(RETRO_ID);
        return retrospective;
    }

    static Retrospective closedRetro() {
        Retrospective retrospective = retro();
        retrospective.close();
        return retrospective;
    }

    static FeedbackItem feedbackItem(String content) {
        FeedbackItem item = new FeedbackItem(retro(), content, FeedbackType.WENT_WELL, ALICE);
        item.setId(FEEDBACK_ID);
        return item;
    }

    static ActionItem actionItem(boolean completed) {
        ActionItem item = new ActionItem(retro(), ACTION_CONTENT, Priority.HIGH, ALICE);
        item.setId(ACTION_ID);
        if (completed) {
            item.markCompleted();
        }
        return item;
    }

}
