package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.Arrays;

import static com.stackcraft.retroflow.service.RetroflowFixtures.RETRO_TITLE;
import static com.stackcraft.retroflow.service.RetroflowFixtures.UNKNOWN_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * test-matrix.md rows 1-5 and 24-26: one OPEN retrospective per team, no reopening, retro input.
 */
@SpringBootTest
@Transactional
class RetrospectiveRulesTest {

    private final RetrospectiveService retrospectiveService;
    private final RetroflowFixtures fixtures;

    @Autowired
    RetrospectiveRulesTest(TeamService teamService, RetrospectiveService retrospectiveService) {
        this.retrospectiveService = retrospectiveService;
        this.fixtures = new RetroflowFixtures(teamService, retrospectiveService);
    }

    @Test
    @DisplayName("Row 1: second OPEN retro for a team is rejected and nothing is saved")
    void createRetrospective_whenTeamHasOpenRetro_throwsConflictAndSavesNothing() {
        // Arrange
        Team team = fixtures.team();
        Retrospective open = fixtures.openRetro(team);

        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.createRetrospective(team.getId(), "Sprint 2 Retro"))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("Team " + team.getId() + ": already has an open retrospective (" + open.getId() + ")");
        assertThat(retrospectiveService.getRetrospectivesForTeam(team.getId())).containsExactly(open);
    }

    @Test
    @DisplayName("Row 2: new retro allowed once the earlier ones are CLOSED")
    void createRetrospective_whenPreviousRetrosClosed_createsOpenRetro() {
        // Arrange
        Team team = fixtures.team();
        retrospectiveService.closeRetrospective(fixtures.openRetro(team).getId());

        // Act
        Retrospective second = retrospectiveService.createRetrospective(team.getId(), "Sprint 2 Retro");

        // Assert
        assertThat(second.getStatus()).isEqualTo(RetrospectiveStatus.OPEN);
        assertThat(retrospectiveService.getRetrospectivesForTeam(team.getId())).hasSize(2);
    }

    @Test
    @DisplayName("Row 3: another team's OPEN retro does not block this team")
    void createRetrospective_whenOtherTeamHasOpenRetro_createsOpenRetro() {
        // Arrange
        fixtures.openRetro(fixtures.team());
        Team team = fixtures.team();

        // Act
        Retrospective retrospective = retrospectiveService.createRetrospective(team.getId(), RETRO_TITLE);

        // Assert
        assertThat(retrospective.getStatus()).isEqualTo(RetrospectiveStatus.OPEN);
        assertThat(retrospective.getTeam().getId()).isEqualTo(team.getId());
    }

    @Test
    @DisplayName("Row 4: closing a CLOSED retro is rejected and it stays CLOSED")
    void closeRetrospective_whenAlreadyClosed_throwsConflict() {
        // Arrange
        Retrospective retrospective = fixtures.closedRetro();

        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.closeRetrospective(retrospective.getId()))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("Retrospective " + retrospective.getId() + ": already closed");
        assertThat(retrospectiveService.getRetrospective(retrospective.getId()).getStatus())
                .isEqualTo(RetrospectiveStatus.CLOSED);
    }

    @Test
    @DisplayName("Row 5: there is no way to reopen: no reopen method, no status setter")
    void reopen_byDesign_hasNoOperationInServiceOrEntity() {
        // Arrange
        Method[] serviceMethods = RetrospectiveService.class.getMethods();
        Method[] entityMethods = Retrospective.class.getMethods();

        // Act
        boolean serviceCanReopen = Arrays.stream(serviceMethods)
                .anyMatch(method -> method.getName().toLowerCase().contains("reopen"));
        boolean entityHasStatusSetter = Arrays.stream(entityMethods)
                .anyMatch(method -> method.getName().equals("setStatus")
                        || method.getName().toLowerCase().contains("reopen"));

        // Assert
        assertThat(serviceCanReopen).isFalse();
        assertThat(entityHasStatusSetter).isFalse();
    }

    @Test
    @DisplayName("Row 24: blank title is rejected")
    void createRetrospective_withBlankTitle_throwsInvalidInput() {
        // Arrange
        Team team = fixtures.team();
        String blankTitle = "   ";

        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.createRetrospective(team.getId(), blankTitle))
                .isExactlyInstanceOf(InvalidInputException.class)
                .hasMessage("Retrospective new: title must not be blank");
    }

    @Test
    @DisplayName("Row 25: without a date the retro is dated today")
    void createRetrospective_withoutDate_usesToday() {
        // Arrange
        Team team = fixtures.team();

        // Act
        Retrospective retrospective = retrospectiveService.createRetrospective(team.getId(), RETRO_TITLE, null);

        // Assert
        assertThat(retrospective.getDate()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("Row 26: creating or listing retros of an unknown team gives 404")
    void createRetrospective_forUnknownTeam_throwsNotFound() {
        // Act & Assert
        assertThatThrownBy(() -> retrospectiveService.createRetrospective(UNKNOWN_ID, RETRO_TITLE))
                .isExactlyInstanceOf(NotFoundException.class)
                .hasMessage("Team " + UNKNOWN_ID + ": not found");
        assertThatThrownBy(() -> retrospectiveService.getRetrospectivesForTeam(UNKNOWN_ID))
                .isExactlyInstanceOf(NotFoundException.class);
    }

}
