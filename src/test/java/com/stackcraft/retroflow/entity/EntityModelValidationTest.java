package com.stackcraft.retroflow.entity;

import jakarta.validation.Validation;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EntityModelValidationTest {

    private static final LocalDate MEETING_DATE = LocalDate.of(2026, 10, 26);
    private static final int OVERLONG_NAME_LENGTH = 201;
    private static final int OVERLONG_CONTENT_LENGTH = 10_001;
    private static ValidatorFactory validatorFactory;

    @BeforeAll
    static void openValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    @ParameterizedTest
    @MethodSource("invalidEntities")
    void validate_whenEntityIsInvalid_reportsConstraint(Object entity, String property) {
        // Arrange
        var validator = validatorFactory.getValidator();

        // Act
        var violations = validator.validate(entity);

        // Assert
        assertThat(violations).anySatisfy(violation ->
            assertThat(violation.getPropertyPath().toString()).startsWith(property));
    }

    static Stream<Arguments> invalidEntities() {
        var retro = retrospective();
        return Stream.of(
            Arguments.of(new Team(" ", List.of("Alice")), "name"),
            Arguments.of(new Team("Alpha", List.of()), "members"),
            Arguments.of(new Team("Alpha", List.of(" ")), "members"),
            Arguments.of(new Team("Alpha", Arrays.asList((String) null)), "members"),
            Arguments.of(new Team("x".repeat(OVERLONG_NAME_LENGTH), List.of("Alice")), "name"),
            Arguments.of(new Team("Alpha", List.of("x".repeat(OVERLONG_NAME_LENGTH))), "members"),
            Arguments.of(new Retrospective(team(), " ", MEETING_DATE), "title"),
            Arguments.of(new Retrospective(team(), "Retro", null), "date"),
            Arguments.of(new Retrospective(team(), "x".repeat(OVERLONG_NAME_LENGTH), MEETING_DATE), "title"),
            Arguments.of(new WentWellFeedback(retro, " ", "Alice"), "content"),
            Arguments.of(new WentWellFeedback(retro, "x".repeat(OVERLONG_CONTENT_LENGTH), "Alice"), "content"),
            Arguments.of(new NeedsImprovementFeedback(retro, "Improve", " "), "submittedBy"),
            Arguments.of(new NeedsImprovementFeedback(retro, "Improve", "x".repeat(OVERLONG_NAME_LENGTH)), "submittedBy"),
            Arguments.of(new ActionItem(retro, "Fix", null, "Alice"), "priority"));
    }

    @Test
    void construct_whenGivenValidValues_setsDefaultsAndRelationships() {
        // Arrange
        var team = team();
        var retro = new Retrospective(team, "Sprint retro", MEETING_DATE);

        // Act
        var action = new ActionItem(retro, "Fix build", Priority.HIGH, "Alice");

        // Assert
        assertThat(retro.getStatus()).isEqualTo(RetrospectiveStatus.OPEN);
        assertThat(action.isCompleted()).isFalse();
        assertThat(team.getRetrospectives()).containsExactly(retro);
        assertThat(retro.getFeedbackItems()).containsExactly(action);
        assertThat(action.getType()).isEqualTo(FeedbackType.ACTION_ITEM);
        assertThat(validatorFactory.getValidator().validate(action)).isEmpty();
    }

    @Test
    void getCollections_whenCallerAttemptsMutation_rejectsChanges() {
        // Arrange
        var inputMembers = new java.util.ArrayList<>(List.of("Alice"));
        var team = new Team("Alpha", inputMembers);
        var retro = new Retrospective(team, "Retro", MEETING_DATE);

        // Act
        inputMembers.clear();

        // Assert
        assertThat(team.getMembers()).containsExactly("Alice");
        assertThatThrownBy(() -> team.getMembers().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> team.getRetrospectives().clear()).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> retro.getFeedbackItems().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void transition_whenClosingAndCompleting_preservesOneWayStates() {
        // Arrange
        var retro = retrospective();
        var action = new ActionItem(retro, "Fix build", Priority.HIGH, "Alice");

        // Act
        action.complete();
        retro.close();

        // Assert
        assertThat(action.isCompleted()).isTrue();
        assertThat(retro.getStatus()).isEqualTo(RetrospectiveStatus.CLOSED);
    }

    private static Team team() {
        return new Team("Alpha", List.of("Alice"));
    }

    private static Retrospective retrospective() {
        return new Retrospective(team(), "Sprint retro", MEETING_DATE);
    }
}
