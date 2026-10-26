package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.stackcraft.retroflow.service.RetroflowFixtures.ALICE;
import static com.stackcraft.retroflow.service.RetroflowFixtures.TWO_MEMBERS;
import static com.stackcraft.retroflow.service.RetroflowFixtures.UNKNOWN_ID;
import static com.stackcraft.retroflow.service.RetroflowFixtures.uniqueTeamName;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * test-matrix.md rows 19-23 (team input) and 32 (unknown ids).
 */
@SpringBootTest
@Transactional
class TeamRulesTest {

    private final TeamService teamService;
    private final RetrospectiveService retrospectiveService;
    private final FeedbackItemService feedbackItemService;
    private final ActionItemService actionItemService;

    @Autowired
    TeamRulesTest(TeamService teamService, RetrospectiveService retrospectiveService,
                  FeedbackItemService feedbackItemService, ActionItemService actionItemService) {
        this.teamService = teamService;
        this.retrospectiveService = retrospectiveService;
        this.feedbackItemService = feedbackItemService;
        this.actionItemService = actionItemService;
    }

    @Test
    @DisplayName("Row 19: a team without members is rejected")
    void createTeam_withoutMembers_throwsInvalidInput() {
        // Arrange
        List<String> noMembers = List.of();

        // Act & Assert
        assertThatThrownBy(() -> teamService.createTeam(uniqueTeamName(), noMembers))
                .isExactlyInstanceOf(InvalidInputException.class)
                .hasMessage("Team new: members must not be empty");
    }

    @Test
    @DisplayName("Row 20: a blank or too long team name is rejected")
    void createTeam_withBlankOrTooLongName_throwsInvalidInput() {
        // Arrange
        String blankName = "  ";
        String tooLongName = "x".repeat(Team.MAX_NAME_LENGTH + 1);

        // Act & Assert
        assertThatThrownBy(() -> teamService.createTeam(blankName, TWO_MEMBERS))
                .isExactlyInstanceOf(InvalidInputException.class)
                .hasMessage("Team new: name must not be blank");
        assertThatThrownBy(() -> teamService.createTeam(tooLongName, TWO_MEMBERS))
                .isExactlyInstanceOf(InvalidInputException.class)
                .hasMessage("Team new: name must be at most " + Team.MAX_NAME_LENGTH + " characters");
    }

    @Test
    @DisplayName("Row 21: a blank member name is rejected")
    void createTeam_withBlankMemberName_throwsInvalidInput() {
        // Arrange
        List<String> membersWithBlank = List.of(ALICE, " ");

        // Act & Assert
        assertThatThrownBy(() -> teamService.createTeam(uniqueTeamName(), membersWithBlank))
                .isExactlyInstanceOf(InvalidInputException.class)
                .hasMessage("Team new: member name must not be blank");
    }

    @Test
    @DisplayName("Row 22: the same member twice (ignoring case) is rejected")
    void createTeam_withDuplicateMemberIgnoringCase_throwsInvalidInput() {
        // Arrange
        List<String> duplicateMembers = List.of(ALICE, "alice");

        // Act & Assert
        assertThatThrownBy(() -> teamService.createTeam(uniqueTeamName(), duplicateMembers))
                .isExactlyInstanceOf(InvalidInputException.class)
                .hasMessage("Team new: member 'alice' is listed more than once");
    }

    @Test
    @DisplayName("Row 23: an existing team name (ignoring case) is a conflict")
    void createTeam_withExistingNameIgnoringCase_throwsConflict() {
        // Arrange
        String name = uniqueTeamName();
        teamService.createTeam(name, TWO_MEMBERS);
        String sameNameOtherCase = name.toUpperCase();

        // Act & Assert
        assertThatThrownBy(() -> teamService.createTeam(sameNameOtherCase, TWO_MEMBERS))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("Team new: a team named '" + sameNameOtherCase + "' already exists");
    }

    @Test
    @DisplayName("Row 32: every lookup by an unknown id gives NotFoundException")
    void lookups_withUnknownId_throwNotFound() {
        // Act & Assert
        assertThatThrownBy(() -> teamService.getTeam(UNKNOWN_ID))
                .isExactlyInstanceOf(NotFoundException.class).hasMessage("Team " + UNKNOWN_ID + ": not found");
        assertThatThrownBy(() -> retrospectiveService.getRetrospective(UNKNOWN_ID))
                .isExactlyInstanceOf(NotFoundException.class).hasMessage("Retrospective " + UNKNOWN_ID + ": not found");
        assertThatThrownBy(() -> retrospectiveService.closeRetrospective(UNKNOWN_ID))
                .isExactlyInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> feedbackItemService.updateFeedbackItem(UNKNOWN_ID, "text"))
                .isExactlyInstanceOf(NotFoundException.class).hasMessage("FeedbackItem " + UNKNOWN_ID + ": not found");
        assertThatThrownBy(() -> feedbackItemService.deleteFeedbackItem(UNKNOWN_ID))
                .isExactlyInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> actionItemService.completeActionItem(UNKNOWN_ID))
                .isExactlyInstanceOf(NotFoundException.class).hasMessage("ActionItem " + UNKNOWN_ID + ": not found");
    }

    @Test
    @DisplayName("Happy path: a valid team is saved with trimmed names")
    void createTeam_withValidInput_savesTrimmedTeam() {
        // Arrange
        String name = uniqueTeamName();

        // Act
        Team team = teamService.createTeam("  " + name + "  ", List.of(" " + ALICE + " "));

        // Assert
        assertThat(teamService.getTeam(team.getId()).getName()).isEqualTo(name);
        assertThat(team.getMembers()).containsExactly(ALICE);
        assertThat(teamService.getAllTeams()).contains(team);
    }

}
