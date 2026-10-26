package com.stackcraft.retroflow.entity;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
class EntityModelPersistenceTest {

    private static final LocalDate MEETING_DATE = LocalDate.of(2026, 10, 26);
    private final EntityManager entityManager;

    @Autowired
    EntityModelPersistenceTest(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Test
    void persist_whenFeedbackHasDifferentTypes_roundTripsPolymorphically() {
        // Arrange
        var retro = persistRetrospective();
        var positive = new WentWellFeedback(retro, "Good release", "Alice");
        var improvement = new NeedsImprovementFeedback(retro, "Slow builds", "Bob");
        var action = new ActionItem(retro, "Fix build", Priority.HIGH, "Alice");

        // Act
        List.of(positive, improvement, action).forEach(entityManager::persist);
        flushAndClear();
        var feedback = entityManager.createQuery("select f from FeedbackItem f order by f.id", FeedbackItem.class)
            .getResultList();

        // Assert
        assertThat(feedback).extracting(FeedbackItem::getType)
            .containsExactly(FeedbackType.WENT_WELL, FeedbackType.NEEDS_IMPROVEMENT, FeedbackType.ACTION_ITEM);
        assertThat(feedback).extracting(Object::getClass)
            .containsExactly(WentWellFeedback.class, NeedsImprovementFeedback.class, ActionItem.class);
    }

    @Test
    void find_whenRelationshipsAreLazy_doesNotLoadParentOrCollections() {
        // Arrange
        var retro = persistRetrospective();
        var action = new ActionItem(retro, "Fix build", Priority.HIGH, "Alice");
        entityManager.persist(action);
        flushAndClear();
        var persistence = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();

        // Act
        var loadedAction = entityManager.find(ActionItem.class, action.getId());
        var parentInitiallyLoaded = persistence.isLoaded(loadedAction, "retrospective");
        var loadedRetro = entityManager.find(Retrospective.class, retro.getId());
        var teamInitiallyLoaded = persistence.isLoaded(loadedRetro, "team");
        var loadedTeam = entityManager.find(Team.class, retro.getTeam().getId());

        // Assert
        assertThat(parentInitiallyLoaded).isFalse();
        assertThat(teamInitiallyLoaded).isFalse();
        assertThat(persistence.isLoaded(loadedRetro, "feedbackItems")).isFalse();
        assertThat(persistence.isLoaded(loadedTeam, "members")).isFalse();
        assertThat(persistence.isLoaded(loadedTeam, "retrospectives")).isFalse();
    }

    @Test
    void persist_whenMembersContainDuplicates_preservesOrderAndDuplicates() {
        // Arrange
        var orderedMembers = List.of("Bob", "Alice", "Bob");
        var team = new Team("Alpha", orderedMembers);

        // Act
        entityManager.persist(team);
        flushAndClear();
        var loadedTeam = entityManager.find(Team.class, team.getId());

        // Assert
        assertThat(loadedTeam.getMembers()).containsExactlyElementsOf(orderedMembers);
    }

    @Test
    void query_whenPriorityAndCompletionAreSpecified_returnsMatchingActions() {
        // Arrange
        var retro = persistRetrospective();
        var completedHigh = new ActionItem(retro, "Fix build", Priority.HIGH, "Alice");
        completedHigh.complete();
        entityManager.persist(completedHigh);
        entityManager.persist(new ActionItem(retro, "Add docs", Priority.LOW, "Bob"));
        entityManager.persist(new ActionItem(retro, "Improve release", Priority.HIGH, "Alice"));
        flushAndClear();

        // Act
        var matches = entityManager.createQuery(
                "select a from ActionItem a where a.priority = :priority and a.completed = :completed",
                ActionItem.class)
            .setParameter("priority", Priority.HIGH).setParameter("completed", true).getResultList();

        // Assert
        assertThat(matches).extracting(ActionItem::getId).containsExactly(completedHigh.getId());
    }

    @Test
    void complete_whenActionIsPersisted_updatesCompletionAndVersion() {
        // Arrange
        var retro = persistRetrospective();
        var action = new ActionItem(retro, "Fix build", Priority.HIGH, "Alice");
        entityManager.persist(action);
        entityManager.flush();
        var originalVersion = action.getVersion();

        // Act
        action.complete();
        retro.close();
        flushAndClear();

        // Assert
        var loadedAction = entityManager.find(ActionItem.class, action.getId());
        assertThat(loadedAction.isCompleted()).isTrue();
        assertThat(loadedAction.getVersion()).isGreaterThan(originalVersion);
        assertThat(entityManager.find(Retrospective.class, retro.getId()).getStatus())
            .isEqualTo(RetrospectiveStatus.CLOSED);
    }

    @Test
    void setProperties_whenEntitiesAreManaged_persistsChanges() {
        // Arrange
        var retro = persistRetrospective();
        var action = new ActionItem(retro, "Fix build", Priority.HIGH, "Alice");
        entityManager.persist(action);
        entityManager.flush();

        // Act
        retro.getTeam().setMembers(List.of("Bob", "Alice"));
        retro.setTitle("Updated retro");
        action.setContent("Improve build");
        action.setSubmittedBy("Bob");
        action.setPriority(Priority.LOW);
        flushAndClear();

        // Assert
        var loadedAction = entityManager.find(ActionItem.class, action.getId());
        assertThat(loadedAction.getContent()).isEqualTo("Improve build");
        assertThat(loadedAction.getSubmittedBy()).isEqualTo("Bob");
        assertThat(loadedAction.getPriority()).isEqualTo(Priority.LOW);
        assertThat(loadedAction.getRetrospective().getTitle()).isEqualTo("Updated retro");
        assertThat(loadedAction.getRetrospective().getTeam().getMembers()).containsExactly("Bob", "Alice");
    }

    @Test
    void insert_whenOrdinaryFeedbackHasActionFields_rejectsInconsistentRow() {
        // Arrange
        var retro = persistRetrospective();
        entityManager.flush();
        var invalidInsert = entityManager.createNativeQuery("""
            insert into feedback_items
                (version, retrospective_id, content, submitted_by, feedback_type, priority, completed)
            values (0, :retroId, 'Good release', 'Alice', 'WENT_WELL', 'HIGH', false)
            """).setParameter("retroId", retro.getId());

        // Act & Assert
        assertThatThrownBy(invalidInsert::executeUpdate).isInstanceOf(jakarta.persistence.PersistenceException.class);
    }

    private Retrospective persistRetrospective() {
        var team = new Team("Alpha", List.of("Alice", "Bob"));
        entityManager.persist(team);
        var retro = new Retrospective(team, "Sprint retro", MEETING_DATE);
        entityManager.persist(retro);
        return retro;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}
