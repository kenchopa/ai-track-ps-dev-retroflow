package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.RetroflowException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static com.stackcraft.retroflow.service.RetroflowFixtures.ALICE;
import static com.stackcraft.retroflow.service.RetroflowFixtures.HIGH;
import static com.stackcraft.retroflow.service.RetroflowFixtures.LOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * test-matrix.md rows 7, 10-12, 15-18, 29 and 34-35: action items, completion and filtering.
 */
@SpringBootTest
@Transactional
class ActionItemRulesTest {

    private static final String CONTENT = "Fix the build";
    private static final String NEW_CONTENT = "Fix the flaky build";

    private final RetrospectiveService retrospectiveService;
    private final ActionItemService actionItemService;
    private final FeedbackItemService feedbackItemService;
    private final RetroflowFixtures fixtures;

    @Autowired
    ActionItemRulesTest(TeamService teamService, RetrospectiveService retrospectiveService,
                        ActionItemService actionItemService, FeedbackItemService feedbackItemService) {
        this.retrospectiveService = retrospectiveService;
        this.actionItemService = actionItemService;
        this.feedbackItemService = feedbackItemService;
        this.fixtures = new RetroflowFixtures(teamService, retrospectiveService);
    }

    @Test
    @DisplayName("Row 7: adding an action item to a CLOSED retro is rejected")
    void addActionItem_whenRetroClosed_throwsConflictAndSavesNothing() {
        // Arrange
        Retrospective closed = fixtures.closedRetro();

        // Act & Assert
        assertThatThrownBy(() -> actionItemService.addActionItem(closed.getId(), CONTENT, HIGH, ALICE))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("Retrospective " + closed.getId() + ": cannot add feedback to a closed retrospective");
        assertThat(feedbackItemService.getFeedbackItems(closed.getId())).isEmpty();
    }

    @Test
    @DisplayName("Row 10: modifying an action item in a CLOSED retro is rejected, values unchanged")
    void updateActionItem_whenRetroClosed_throwsConflictAndKeepsValues() {
        // Arrange
        ActionItem item = fixtures.actionItemInClosedRetro();

        // Act & Assert
        assertThatThrownBy(() -> actionItemService.updateActionItem(item.getId(), NEW_CONTENT, LOW))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("ActionItem " + item.getId() + ": cannot modify an item of a closed retrospective");
        assertThat(item.getContent()).isEqualTo(CONTENT);
        assertThat(item.getPriority()).isEqualTo(Priority.HIGH);
    }

    @Test
    @DisplayName("Row 11: deleting an action item in a CLOSED retro is rejected, item still exists")
    void deleteFeedbackItem_onActionItemWhenRetroClosed_throwsConflictAndKeepsItem() {
        // Arrange
        ActionItem item = fixtures.actionItemInClosedRetro();

        // Act & Assert
        assertThatThrownBy(() -> feedbackItemService.deleteFeedbackItem(item.getId()))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("ActionItem " + item.getId() + ": cannot delete an item of a closed retrospective");
        assertThat(feedbackItemService.getFeedbackItems(item.getRetrospective().getId())).containsExactly(item);
    }

    @Test
    @DisplayName("Row 12: completing an action item stays allowed after its retro is CLOSED (D-F6)")
    void completeActionItem_whenRetroClosed_completesItem() {
        // Arrange
        ActionItem item = fixtures.actionItemInClosedRetro();

        // Act
        ActionItem completed = actionItemService.completeActionItem(item.getId());

        // Assert
        assertThat(completed.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("Row 15: uncompleting a completed action item is rejected and it stays completed")
    void uncompleteActionItem_whenCompleted_throwsConflictAndStaysCompleted() {
        // Arrange
        ActionItem item = fixtures.actionItem(fixtures.openRetro(), HIGH);
        actionItemService.completeActionItem(item.getId());

        // Act & Assert
        assertThatThrownBy(() -> actionItemService.uncompleteActionItem(item.getId()))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("ActionItem " + item.getId() + ": already completed, cannot be undone");
        assertThat(item.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("Row 16: completing an already completed action item is rejected")
    void completeActionItem_whenAlreadyCompleted_throwsConflict() {
        // Arrange
        ActionItem item = fixtures.actionItem(fixtures.openRetro(), HIGH);
        actionItemService.completeActionItem(item.getId());

        // Act & Assert
        assertThatThrownBy(() -> actionItemService.completeActionItem(item.getId()))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("ActionItem " + item.getId() + ": already completed");
    }

    @Test
    @DisplayName("Row 17: uncompleting an open action item changes nothing")
    void uncompleteActionItem_whenNotCompleted_returnsItemUnchanged() {
        // Arrange
        ActionItem item = fixtures.actionItem(fixtures.openRetro(), HIGH);

        // Act
        ActionItem result = actionItemService.uncompleteActionItem(item.getId());

        // Assert
        assertThat(result.isCompleted()).isFalse();
    }

    @Test
    @DisplayName("Row 18: completing an open action item marks it completed")
    void completeActionItem_whenNotCompleted_marksCompleted() {
        // Arrange
        ActionItem item = fixtures.actionItem(fixtures.openRetro(), HIGH);

        // Act
        ActionItem completed = actionItemService.completeActionItem(item.getId());

        // Assert
        assertThat(completed.isCompleted()).isTrue();
    }

    @Test
    @DisplayName("Row 29: unknown or missing priority is rejected")
    void addActionItem_withUnknownPriority_throwsInvalidInput() {
        // Arrange
        Retrospective retrospective = fixtures.openRetro();
        String unknownPriority = "URGENT";

        // Act & Assert
        assertThatThrownBy(() -> actionItemService.addActionItem(retrospective.getId(), CONTENT, unknownPriority, ALICE))
                .isExactlyInstanceOf(InvalidInputException.class)
                .hasMessage("ActionItem new: priority must be one of LOW, MEDIUM, HIGH");
        assertThatThrownBy(() -> actionItemService.addActionItem(retrospective.getId(), CONTENT, null, ALICE))
                .isExactlyInstanceOf(InvalidInputException.class);
    }

    @Test
    @DisplayName("Row 34: filters by priority and completed, across all retros of the team")
    void getActionItemsForTeam_withFilters_returnsOnlyMatchingItems() {
        // Arrange
        Team team = fixtures.team();
        Retrospective first = fixtures.openRetro(team);
        ActionItem highDone = fixtures.actionItem(first, HIGH);
        actionItemService.completeActionItem(highDone.getId());
        retrospectiveService.closeRetrospective(first.getId());
        Retrospective second = fixtures.openRetro(team);
        ActionItem highOpen = fixtures.actionItem(second, HIGH);
        ActionItem lowOpen = fixtures.actionItem(second, LOW);

        // Act & Assert
        assertThat(actionItemService.getActionItemsForTeam(team.getId(), HIGH, null)).containsExactly(highDone, highOpen);
        assertThat(actionItemService.getActionItemsForTeam(team.getId(), null, true)).containsExactly(highDone);
        assertThat(actionItemService.getActionItemsForTeam(team.getId(), HIGH, false)).containsExactly(highOpen);
        assertThat(actionItemService.getActionItemsForTeam(team.getId(), null, null))
                .containsExactly(highDone, highOpen, lowOpen);
    }

    @Test
    @DisplayName("Row 35: an unknown priority filter is rejected")
    void getActionItemsForTeam_withUnknownPriority_throwsInvalidInput() {
        // Arrange
        Team team = fixtures.team();

        // Act & Assert
        assertThatThrownBy(() -> actionItemService.getActionItemsForTeam(team.getId(), "URGENT", null))
                .isExactlyInstanceOf(InvalidInputException.class)
                .hasMessage("Team " + team.getId() + ": priority filter must be one of LOW, MEDIUM, HIGH");
    }

}
