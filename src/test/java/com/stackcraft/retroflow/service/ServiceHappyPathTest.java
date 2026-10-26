package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.entity.Retrospective;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static com.stackcraft.retroflow.service.RetroflowFixtures.ALICE;
import static com.stackcraft.retroflow.service.RetroflowFixtures.HIGH;
import static com.stackcraft.retroflow.service.RetroflowFixtures.LOW;
import static com.stackcraft.retroflow.service.RetroflowFixtures.WENT_WELL;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Happy paths that PIT showed were never exercised: with these lines
 * untested, an update that changes nothing or a method that returns null
 * would have gone unnoticed (see prompt-log.md, AC step 4).
 */
@SpringBootTest
@Transactional
class ServiceHappyPathTest {

    private static final String CONTENT = "Fix the build";
    private static final String NEW_CONTENT = "Fix the flaky build";

    private final RetrospectiveService retrospectiveService;
    private final ActionItemService actionItemService;
    private final RetroflowFixtures fixtures;

    @Autowired
    ServiceHappyPathTest(TeamService teamService, RetrospectiveService retrospectiveService,
                         ActionItemService actionItemService) {
        this.retrospectiveService = retrospectiveService;
        this.actionItemService = actionItemService;
        this.fixtures = new RetroflowFixtures(teamService, retrospectiveService);
    }

    @Test
    void updateActionItem_whenRetroOpen_updatesContentAndPriority() {
        // Arrange
        ActionItem item = fixtures.actionItem(fixtures.openRetro(), HIGH);

        // Act
        ActionItem updated = actionItemService.updateActionItem(item.getId(), NEW_CONTENT, LOW);

        // Assert
        assertThat(updated.getContent()).isEqualTo(NEW_CONTENT);
        assertThat(updated.getPriority()).isEqualTo(Priority.LOW);
    }

    @Test
    void addFeedbackItem_whenRetroOpen_returnsSavedItem() {
        // Arrange
        Retrospective retrospective = fixtures.openRetro();

        // Act
        FeedbackItem item = retrospectiveService.addFeedbackItem(retrospective.getId(), CONTENT, WENT_WELL, ALICE);

        // Assert
        assertThat(item.getId()).isNotNull();
        assertThat(item.getContent()).isEqualTo(CONTENT);
        assertThat(item.getType()).isEqualTo(FeedbackType.WENT_WELL);
        assertThat(item.getAuthor()).isEqualTo(ALICE);
    }

    @Test
    void uncompleteActionItem_whenNotCompleted_returnsSameItem() {
        // Arrange
        ActionItem item = fixtures.actionItem(fixtures.openRetro(), HIGH);

        // Act
        ActionItem result = retrospectiveService.uncompleteActionItem(item.getId());

        // Assert
        assertThat(result).isSameAs(item);
        assertThat(result.isCompleted()).isFalse();
    }

}
