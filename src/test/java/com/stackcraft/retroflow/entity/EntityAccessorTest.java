package com.stackcraft.retroflow.entity;

import com.stackcraft.retroflow.exception.RetroflowException;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityAccessorTest {

    private static final LocalDate MEETING_DATE = LocalDate.of(2026, 10, 26);
    private static final long ENTITY_ID = 42L;
    private static final long ENTITY_VERSION = 3L;

    @Test
    void construct_whenOrdinarySubtypesUseAllArguments_preservesMetadataAndType() {
        // Arrange
        var retro = new Retrospective(team(), "Retro", MEETING_DATE);

        // Act
        var positive = new WentWellFeedback(ENTITY_ID, ENTITY_VERSION, retro, "Good release", "Alice");
        var improvement = new NeedsImprovementFeedback(ENTITY_ID, ENTITY_VERSION, retro, "Slow builds", "Alice");

        // Assert
        assertThat(positive.getId()).isEqualTo(ENTITY_ID);
        assertThat(positive.getVersion()).isEqualTo(ENTITY_VERSION);
        assertThat(positive.getType()).isEqualTo(FeedbackType.WENT_WELL);
        assertThat(improvement.getId()).isEqualTo(ENTITY_ID);
        assertThat(improvement.getVersion()).isEqualTo(ENTITY_VERSION);
        assertThat(improvement.getType()).isEqualTo(FeedbackType.NEEDS_IMPROVEMENT);
    }

    @Test
    void construct_whenAllArgumentsAreSupplied_setsInheritedAndDeclaredFields() {
        // Arrange
        var team = new Team(ENTITY_ID, ENTITY_VERSION, "Alpha", List.of("Alice"), List.of());
        var retro = new Retrospective(ENTITY_ID, ENTITY_VERSION, team, "Retro", MEETING_DATE,
            RetrospectiveStatus.OPEN, List.of());

        // Act
        var action = new ActionItem(ENTITY_ID, ENTITY_VERSION, retro, "Fix build", "Alice", Priority.HIGH, true);

        // Assert
        assertThat(team.getId()).isEqualTo(ENTITY_ID);
        assertThat(team.getVersion()).isEqualTo(ENTITY_VERSION);
        assertThat(retro.getId()).isEqualTo(ENTITY_ID);
        assertThat(retro.getVersion()).isEqualTo(ENTITY_VERSION);
        assertThat(action.getId()).isEqualTo(ENTITY_ID);
        assertThat(action.getVersion()).isEqualTo(ENTITY_VERSION);
        assertThat(action.getContent()).isEqualTo("Fix build");
        assertThat(action.getSubmittedBy()).isEqualTo("Alice");
        assertThat(action.getPriority()).isEqualTo(Priority.HIGH);
        assertThat(action.isCompleted()).isTrue();
    }

    @Test
    void setProperties_whenUsingNoArgsConstructors_populatesEntityGraph() {
        // Arrange
        var team = new Team();
        var retro = new Retrospective();
        var feedback = new WentWellFeedback();

        // Act
        team.setName("Alpha");
        team.setMembers(List.of("Alice"));
        retro.setTeam(team);
        retro.setTitle("Retro");
        retro.setDate(MEETING_DATE);
        feedback.setRetrospective(retro);
        feedback.setContent("Good release");
        feedback.setSubmittedBy("Alice");

        // Assert
        assertThat(retro.getTeam().getName()).isEqualTo("Alpha");
        assertThat(retro.getTitle()).isEqualTo("Retro");
        assertThat(retro.getDate()).isEqualTo(MEETING_DATE);
        assertThat(retro.getFeedbackItems()).containsExactly(feedback);
    }

    @Test
    void setCollections_whenPassedOwnViews_preservesContents() {
        // Arrange
        var team = team();
        var retro = new Retrospective(team, "Retro", MEETING_DATE);
        var action = new ActionItem(retro, "Fix build", Priority.HIGH, "Alice");

        // Act
        team.setMembers(team.getMembers());
        team.setRetrospectives(team.getRetrospectives());
        retro.setFeedbackItems(retro.getFeedbackItems());
        retro.setTeam(team);
        action.setRetrospective(retro);

        // Assert
        assertThat(team.getMembers()).containsExactly("Alice");
        assertThat(team.getRetrospectives()).containsExactly(retro);
        assertThat(retro.getFeedbackItems()).containsExactly(action);
    }

    @Test
    void setParents_whenReparenting_rejectsChangesWithoutChangingGraph() {
        // Arrange
        var retro = new Retrospective(team(), "Retro", MEETING_DATE);
        var otherTeam = team();
        var otherRetro = new Retrospective(otherTeam, "Other", MEETING_DATE);
        var feedback = new WentWellFeedback(retro, "Good release", "Alice");

        // Act & Assert
        assertThatThrownBy(() -> retro.setTeam(otherTeam)).isInstanceOf(RetroflowException.class);
        assertThatThrownBy(() -> feedback.setRetrospective(otherRetro)).isInstanceOf(RetroflowException.class);
        assertThat(feedback.getRetrospective()).isSameAs(retro);
        assertThat(otherTeam.getRetrospectives()).containsExactly(otherRetro);
        assertThat(otherRetro.getFeedbackItems()).isEmpty();
    }

    @Test
    void setCollections_whenRemovingOwnedChildren_rejectsInconsistentGraph() {
        // Arrange
        var team = team();
        var retro = new Retrospective(team, "Retro", MEETING_DATE);
        var action = new ActionItem(retro, "Fix build", Priority.HIGH, "Alice");

        // Act & Assert
        assertThatThrownBy(() -> team.setRetrospectives(List.of())).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> retro.setFeedbackItems(List.of())).isInstanceOf(RetroflowException.class);
        assertThat(team.getRetrospectives()).containsExactly(retro);
        assertThat(retro.getFeedbackItems()).containsExactly(action);
    }

    @Test
    void setLifecycle_whenReversingTerminalStates_throwsFormattedException() {
        // Arrange
        var retro = new Retrospective(team(), "Retro", MEETING_DATE);
        var action = new ActionItem(retro, "Fix build", Priority.HIGH, "Alice");
        retro.setId(ENTITY_ID);
        action.setId(ENTITY_ID);
        action.complete();
        retro.close();

        // Act & Assert
        assertThatThrownBy(() -> retro.setStatus(RetrospectiveStatus.OPEN))
            .isInstanceOf(RetroflowException.class).hasMessageStartingWith("Retrospective " + ENTITY_ID + ": ");
        assertThatThrownBy(() -> action.setCompleted(false))
            .isInstanceOf(RetroflowException.class).hasMessageStartingWith("ActionItem " + ENTITY_ID + ": ");
        assertThat(retro.getStatus()).isEqualTo(RetrospectiveStatus.CLOSED);
        assertThat(action.isCompleted()).isTrue();
    }

    @Test
    void setFeedback_whenRetrospectiveIsClosed_rejectsEditsAndNewItems() {
        // Arrange
        var retro = new Retrospective(team(), "Retro", MEETING_DATE);
        var action = new ActionItem(retro, "Fix build", Priority.HIGH, "Alice");
        retro.close();

        // Act & Assert
        assertThatThrownBy(() -> action.setContent("Changed")).isInstanceOf(RetroflowException.class);
        assertThatThrownBy(() -> action.setSubmittedBy("Bob")).isInstanceOf(RetroflowException.class);
        assertThatThrownBy(() -> action.setPriority(Priority.LOW)).isInstanceOf(RetroflowException.class);
        assertThatThrownBy(() -> new WentWellFeedback(retro, "Late feedback", "Alice"))
            .isInstanceOf(RetroflowException.class);
        assertThat(retro.getFeedbackItems()).containsExactly(action);
        assertThat(action.getContent()).isEqualTo("Fix build");
        assertThat(action.getPriority()).isEqualTo(Priority.HIGH);
    }

    private Team team() {
        return new Team("Alpha", List.of("Alice"));
    }
}
