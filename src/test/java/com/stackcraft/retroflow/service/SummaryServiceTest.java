package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.exception.SummaryUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

import static com.stackcraft.retroflow.exception.SummaryUnavailableException.Reason.INVALID_ANSWER;
import static com.stackcraft.retroflow.exception.SummaryUnavailableException.Reason.TIMEOUT;
import static com.stackcraft.retroflow.service.RetroflowFixtures.ALICE;
import static com.stackcraft.retroflow.service.RetroflowFixtures.HIGH;
import static com.stackcraft.retroflow.service.RetroflowFixtures.LOW;
import static com.stackcraft.retroflow.service.RetroflowFixtures.NEEDS_IMPROVEMENT;
import static com.stackcraft.retroflow.service.RetroflowFixtures.UNKNOWN_ID;
import static com.stackcraft.retroflow.service.RetroflowFixtures.WENT_WELL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * The summary rule (only a CLOSED retrospective) and the validation of the
 * model's answer, with a stub instead of the model. Not @Transactional: the
 * summary runs outside a transaction, so the test data has to be committed.
 */
@SpringBootTest(properties = "retroflow.summary.max-items=" + SummaryServiceTest.MAX_ITEMS)
@Import(StubSummaryModel.Config.class)
class SummaryServiceTest {

    static final int MAX_ITEMS = 4;
    private static final String VALID = """
            {"wentWell": ["Pairing helped"], "needsImprovement": ["Builds are slow"],
             "openActionItems": [{"id": %d, "summary": "Speed up the build", "priority": "%s"}]}""";

    private final RetrospectiveService retrospectiveService;
    private final FeedbackItemService feedbackItemService;
    private final StubSummaryModel model;
    private final RetroflowFixtures fixtures;
    private Retrospective retro;
    private Long actionId;

    @Autowired
    SummaryServiceTest(TeamService teamService, RetrospectiveService retrospectiveService,
                       FeedbackItemService feedbackItemService, StubSummaryModel model) {
        this.retrospectiveService = retrospectiveService;
        this.feedbackItemService = feedbackItemService;
        this.model = model;
        this.fixtures = new RetroflowFixtures(teamService, retrospectiveService);
    }

    /** A retro with one item of each kind plus one completed action item; closed by each test that needs it. */
    @BeforeEach
    void openRetroWithFeedback() {
        model.reset();
        retro = fixtures.openRetro();
        retrospectiveService.addFeedbackItem(retro.getId(), "Pairing worked well", WENT_WELL, ALICE);
        retrospectiveService.addFeedbackItem(retro.getId(), "The build is slow", NEEDS_IMPROVEMENT, ALICE);
        actionId = fixtures.actionItem(retro, HIGH).getId();
        retrospectiveService.completeActionItem(fixtures.actionItem(retro, LOW).getId());
    }

    @Test
    void summarise_whenRetroOpen_throwsConflictAndNeverCallsTheModel() {
        // Act & Assert
        assertThatThrownBy(() -> feedbackItemService.summarise(retro.getId()))
                .isExactlyInstanceOf(RetroflowException.class)
                .hasMessage("Retrospective " + retro.getId() + ": only a closed retrospective can be summarised");
        assertThat(model.calls()).isEmpty();
    }

    @Test
    void summarise_whenRetroUnknown_throwsNotFound() {
        // Act & Assert
        assertThatThrownBy(() -> feedbackItemService.summarise(UNKNOWN_ID))
                .isExactlyInstanceOf(NotFoundException.class)
                .hasMessage("Retrospective " + UNKNOWN_ID + ": not found");
    }

    @Test
    void summarise_whenClosedAndAnswerValid_returnsSummaryWithOpenActionItemsOnly() {
        // Arrange
        retrospectiveService.closeRetrospective(retro.getId());
        model.answers(input -> VALID.formatted(actionId, HIGH));

        // Act
        RetrospectiveSummary summary = feedbackItemService.summarise(retro.getId());

        // Assert
        assertThat(summary.wentWell()).containsExactly("Pairing helped");
        assertThat(summary.needsImprovement()).containsExactly("Builds are slow");
        assertThat(summary.openActionItems())
                .containsExactly(new RetrospectiveSummary.OpenActionItem(actionId, "Speed up the build", Priority.HIGH));
    }

    @Test
    void summarise_whenClosed_sendsItemsWithoutAuthorsToTheModel() {
        // Arrange
        retrospectiveService.closeRetrospective(retro.getId());
        model.answers(input -> VALID.formatted(actionId, HIGH));

        // Act
        feedbackItemService.summarise(retro.getId());

        // Assert: four items, as data; the record has no author field, so no names leave RetroFlow
        SummaryModel.Input sent = model.calls().getFirst();
        assertThat(sent.items()).extracting(SummaryModel.Item::type)
                .containsExactly(WENT_WELL, NEEDS_IMPROVEMENT, "ACTION_ITEM", "ACTION_ITEM");
        assertThat(sent.items().toString()).doesNotContain(ALICE);
    }

    @ParameterizedTest(name = "rejects: {0}")
    @ValueSource(strings = {
            "this is not JSON",
            "[]",
            "{\"wentWell\": [\"Pairing helped\"], \"openActionItems\": []}",
            "{\"wentWell\": [\"Pairing helped\"], \"needsImprovement\": [], \"openActionItems\": [{\"id\": %d, \"summary\": \"x\", \"priority\": \"HIGH\"}]}",
            "{\"wentWell\": [\"Pairing helped\"], \"needsImprovement\": [\"Slow\"], \"openActionItems\": [{\"id\": %d, \"summary\": \"x\", \"priority\": \"URGENT\"}]}",
            "{\"wentWell\": [\"Pairing helped\"], \"needsImprovement\": [\"Slow\"], \"openActionItems\": [{\"id\": %d, \"summary\": \"x\", \"priority\": \"LOW\"}]}",
            "{\"wentWell\": [\"Pairing helped\"], \"needsImprovement\": [\"Slow\"], \"openActionItems\": []}",
            "{\"wentWell\": [\" \"], \"needsImprovement\": [\"Slow\"], \"openActionItems\": [{\"id\": %d, \"summary\": \"x\", \"priority\": \"HIGH\"}]}",
            "{\"wentWell\": [\"a\", \"b\"], \"needsImprovement\": [\"Slow\"], \"openActionItems\": [{\"id\": %d, \"summary\": \"x\", \"priority\": \"HIGH\"}]}"
    })
    void summarise_whenAnswerDoesNotHoldUp_throwsInvalidAnswer(String answer) {
        // Arrange: not JSON, not an object, a missing list, an emptied improvement list (prompt injection),
        // an unknown enum, a wrong priority, a missing action item, a blank point, more points than items
        retrospectiveService.closeRetrospective(retro.getId());
        model.answers(input -> answer.formatted(actionId));

        // Act & Assert
        assertThatThrownBy(() -> feedbackItemService.summarise(retro.getId()))
                .isExactlyInstanceOf(SummaryUnavailableException.class)
                .hasMessage("Retrospective " + retro.getId() + ": the summary service gave an answer we could not use, try again")
                .extracting("reason").isEqualTo(INVALID_ANSWER);
    }

    @Test
    void summarise_whenModelTimesOut_passesTheTimeoutOn() {
        // Arrange
        retrospectiveService.closeRetrospective(retro.getId());
        model.fails(TIMEOUT);

        // Act & Assert
        assertThatThrownBy(() -> feedbackItemService.summarise(retro.getId()))
                .isExactlyInstanceOf(SummaryUnavailableException.class)
                .extracting("reason").isEqualTo(TIMEOUT);
    }

    @Test
    void summarise_whenMoreItemsThanTheLimit_throwsInvalidInputAndNeverCallsTheModel() {
        // Arrange: four items now, one more makes five, over the limit of four
        retrospectiveService.addFeedbackItem(retro.getId(), "Coffee was good", WENT_WELL, ALICE);
        retrospectiveService.closeRetrospective(retro.getId());

        // Act & Assert
        assertThatThrownBy(() -> feedbackItemService.summarise(retro.getId()))
                .isExactlyInstanceOf(InvalidInputException.class)
                .hasMessage("Retrospective " + retro.getId() + ": has 5 feedback items, a summary takes at most " + MAX_ITEMS);
        assertThat(model.calls()).isEmpty();
    }

}
