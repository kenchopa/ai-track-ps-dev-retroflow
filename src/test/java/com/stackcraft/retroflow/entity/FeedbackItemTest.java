package com.stackcraft.retroflow.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * audit-findings.md C1: the constructor guard that keeps ACTION_ITEM out of a
 * plain FeedbackItem (decisions.md E2) must word its message the §2.1 way.
 */
class FeedbackItemTest {

    private static final String ALICE = "Alice";
    private static final String CONTENT = "Pairing worked well";
    private static final String GUARD_MESSAGE = "FeedbackItem new: type ACTION_ITEM must be created as an ActionItem";

    private static Retrospective retrospective() {
        Team team = new Team("Team Feedback", List.of(ALICE));
        return new Retrospective(team, "Sprint 1", LocalDate.now());
    }

    @Test
    @DisplayName("C1: a plain FeedbackItem of type ACTION_ITEM is refused with a §2.1 message")
    void constructor_withActionItemType_throwsIllegalArgumentWithFormattedMessage() {
        // Arrange
        Retrospective retrospective = retrospective();

        // Act & Assert
        assertThatThrownBy(() -> new FeedbackItem(retrospective, CONTENT, FeedbackType.ACTION_ITEM, ALICE))
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessage(GUARD_MESSAGE);
    }

    @ParameterizedTest
    @EnumSource(value = FeedbackType.class, names = "ACTION_ITEM", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("C1 boundary: the other types still pass the guard and are kept as given")
    void constructor_withNonActionItemType_keepsTypeAndFields(FeedbackType allowedType) {
        // Arrange
        Retrospective retrospective = retrospective();

        // Act
        FeedbackItem item = new FeedbackItem(retrospective, CONTENT, allowedType, ALICE);

        // Assert
        assertThat(item.getType()).isEqualTo(allowedType);
        assertThat(item.getContent()).isEqualTo(CONTENT);
        assertThat(item.getAuthor()).isEqualTo(ALICE);
        assertThat(item.getRetrospective()).isSameAs(retrospective);
    }

}
