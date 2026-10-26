package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.RetroflowException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static com.stackcraft.retroflow.service.RetroflowFixtures.ALICE;
import static com.stackcraft.retroflow.service.RetroflowFixtures.HIGH;
import static com.stackcraft.retroflow.service.RetroflowFixtures.NEEDS_IMPROVEMENT;
import static com.stackcraft.retroflow.service.RetroflowFixtures.WENT_WELL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * test-matrix.md rows 6, 8-9, 13-14, 27-28, 30-31 and 33: feedback items and the closed-retro freeze.
 */
@SpringBootTest
@Transactional
class FeedbackItemRulesTest {

    private static final String ORIGINAL = "Pairing worked well";
    private static final String CHANGED = "Pairing worked really well";

    private final RetrospectiveService retrospectiveService;
    private final FeedbackItemService feedbackItemService;
    private final RetroflowFixtures fixtures;

    @Autowired
    FeedbackItemRulesTest(TeamService teamService, RetrospectiveService retrospectiveService,
                          FeedbackItemService feedbackItemService) {
        this.retrospectiveService = retrospectiveService;
        this.feedbackItemService = feedbackItemService;
        this.fixtures = new RetroflowFixtures(teamService, retrospectiveService);
    }

    private FeedbackItem itemInClosedRetro() {
        Retrospective retrospective = fixtures.openRetro();
        FeedbackItem item = feedbackItemService.addFeedbackItem(retrospective.getId(), ORIGINAL, WENT_WELL, ALICE);
        retrospectiveService.closeRetrospective(retrospective.getId());
        return item;
    }

    @Test
    @DisplayName("Row 6: adding feedback to a CLOSED retro is rejected and nothing is saved")
    void addFeedbackItem_whenRetroClosed_throwsConflictAndSavesNothing() {
        // Arrange
        Retrospective closed = fixtures.closedRetro();

        // Act & Assert
        assertThatThrownBy(() -> feedbackItemService.addFeedbackItem(closed.getId(), ORIGINAL, WENT_WELL, ALICE))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("Retrospective " + closed.getId() + ": cannot add feedback to a closed retrospective");
        assertThat(feedbackItemService.getFeedbackItems(closed.getId())).isEmpty();
    }

    @Test
    @DisplayName("Row 8: modifying feedback in a CLOSED retro is rejected, content unchanged")
    void updateFeedbackItem_whenRetroClosed_throwsConflictAndKeepsContent() {
        // Arrange
        FeedbackItem item = itemInClosedRetro();

        // Act & Assert
        assertThatThrownBy(() -> feedbackItemService.updateFeedbackItem(item.getId(), CHANGED))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("FeedbackItem " + item.getId() + ": cannot modify an item of a closed retrospective");
        assertThat(item.getContent()).isEqualTo(ORIGINAL);
    }

    @Test
    @DisplayName("Row 9: deleting feedback in a CLOSED retro is rejected, item still exists")
    void deleteFeedbackItem_whenRetroClosed_throwsConflictAndKeepsItem() {
        // Arrange
        FeedbackItem item = itemInClosedRetro();

        // Act & Assert
        assertThatThrownBy(() -> feedbackItemService.deleteFeedbackItem(item.getId()))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("FeedbackItem " + item.getId() + ": cannot delete an item of a closed retrospective");
        assertThat(feedbackItemService.getFeedbackItems(item.getRetrospective().getId())).containsExactly(item);
    }

    @Test
    @DisplayName("Row 13: modifying feedback in an OPEN retro updates the content")
    void updateFeedbackItem_whenRetroOpen_updatesContent() {
        // Arrange
        Retrospective retrospective = fixtures.openRetro();
        FeedbackItem item = feedbackItemService.addFeedbackItem(retrospective.getId(), ORIGINAL, WENT_WELL, ALICE);

        // Act
        FeedbackItem updated = feedbackItemService.updateFeedbackItem(item.getId(), "  " + CHANGED + "  ");

        // Assert
        assertThat(updated.getContent()).isEqualTo(CHANGED);
        assertThat(updated.getType()).isEqualTo(FeedbackType.WENT_WELL);
    }

    @Test
    @DisplayName("Row 14: deleting feedback in an OPEN retro removes it")
    void deleteFeedbackItem_whenRetroOpen_removesItem() {
        // Arrange
        Retrospective retrospective = fixtures.openRetro();
        FeedbackItem kept = feedbackItemService.addFeedbackItem(retrospective.getId(), ORIGINAL, WENT_WELL, ALICE);
        FeedbackItem removed = feedbackItemService.addFeedbackItem(retrospective.getId(), CHANGED, WENT_WELL, ALICE);

        // Act
        feedbackItemService.deleteFeedbackItem(removed.getId());

        // Assert
        assertThat(feedbackItemService.getFeedbackItems(retrospective.getId())).containsExactly(kept);
    }

    @ParameterizedTest
    @ValueSource(strings = {"GREAT", "went_well"})
    @DisplayName("Row 27: unknown feedback type is rejected and the allowed values are listed")
    void addFeedbackItem_withUnknownType_throwsInvalidInput(String unknownType) {
        // Arrange
        Retrospective retrospective = fixtures.openRetro();

        // Act & Assert
        assertThatThrownBy(() -> feedbackItemService.addFeedbackItem(retrospective.getId(), ORIGINAL, unknownType, ALICE))
                .isExactlyInstanceOf(InvalidInputException.class)
                .hasMessage("FeedbackItem new: type must be one of WENT_WELL, NEEDS_IMPROVEMENT");
    }

    @Test
    @DisplayName("Row 28: ACTION_ITEM through the feedback operation is rejected")
    void addFeedbackItem_withActionItemType_throwsInvalidInput() {
        // Arrange
        Retrospective retrospective = fixtures.openRetro();

        // Act & Assert
        assertThatThrownBy(() -> feedbackItemService.addFeedbackItem(retrospective.getId(), ORIGINAL, "ACTION_ITEM", ALICE))
                .isExactlyInstanceOf(InvalidInputException.class)
                .hasMessage("FeedbackItem new: type ACTION_ITEM must be added as an action item");
    }

    @Test
    @DisplayName("Row 30: an author who is not a team member is rejected")
    void addFeedbackItem_byNonMember_throwsInvalidInput() {
        // Arrange
        Retrospective retrospective = fixtures.openRetro();
        String outsider = "Zoe";

        // Act & Assert
        assertThatThrownBy(() -> feedbackItemService.addFeedbackItem(retrospective.getId(), ORIGINAL, WENT_WELL, outsider))
                .isExactlyInstanceOf(InvalidInputException.class)
                .hasMessage("Team " + retrospective.getTeam().getId() + ": 'Zoe' is not a member");
    }

    @Test
    @DisplayName("Row 31: blank content is rejected")
    void addFeedbackItem_withBlankContent_throwsInvalidInput() {
        // Arrange
        Retrospective retrospective = fixtures.openRetro();
        String blankContent = " ";

        // Act & Assert
        assertThatThrownBy(() -> feedbackItemService.addFeedbackItem(retrospective.getId(), blankContent, WENT_WELL, ALICE))
                .isExactlyInstanceOf(InvalidInputException.class)
                .hasMessage("FeedbackItem new: content must not be blank");
    }

    @Test
    @DisplayName("Row 33: listing a retro's feedback returns all three types in order")
    void getFeedbackItems_withAllTypes_returnsAllInCreationOrder() {
        // Arrange
        Retrospective retrospective = fixtures.openRetro();
        FeedbackItem wentWell = feedbackItemService.addFeedbackItem(retrospective.getId(), ORIGINAL, WENT_WELL, ALICE);
        FeedbackItem improve = feedbackItemService.addFeedbackItem(retrospective.getId(), CHANGED, NEEDS_IMPROVEMENT, ALICE);
        ActionItem action = fixtures.actionItem(retrospective, HIGH);

        // Act
        var items = feedbackItemService.getFeedbackItems(retrospective.getId());

        // Assert
        assertThat(items).containsExactly(wentWell, improve, action);
        assertThat(items.get(2)).isInstanceOf(ActionItem.class);
    }

}
