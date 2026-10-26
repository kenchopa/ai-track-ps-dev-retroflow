package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.exception.SummaryUnavailableException;
import com.stackcraft.retroflow.exception.SummaryUnavailableException.Reason;
import com.stackcraft.retroflow.service.RetrospectiveSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contract of POST /api/retrospectives/{id}/summary: JSON out, every failure
 * of the model as a status the caller can act on, in the StackCraft error format.
 */
@WebMvcTest
class SummaryControllerTest extends ControllerContractTest {

    private static final String SUMMARY_URL = "/api/retrospectives/" + WebFixtures.RETRO_ID + "/summary";
    private static final String PREFIX = "Retrospective " + WebFixtures.RETRO_ID + ": ";

    @Test
    void summarise_whenRetroClosed_returns200WithSummary() throws Exception {
        // Arrange
        var summary = new RetrospectiveSummary(List.of("Pairing helped"), List.of("Builds are slow"),
                List.of(new RetrospectiveSummary.OpenActionItem(WebFixtures.ACTION_ID, "Speed up the build", Priority.HIGH)));
        when(feedbackItemService.summarise(WebFixtures.RETRO_ID)).thenReturn(summary);

        // Act & Assert
        mvc.perform(post(SUMMARY_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.wentWell[0]").value("Pairing helped"))
                .andExpect(jsonPath("$.needsImprovement[0]").value("Builds are slow"))
                .andExpect(jsonPath("$.openActionItems[0].id").value(WebFixtures.ACTION_ID))
                .andExpect(jsonPath("$.openActionItems[0].priority").value("HIGH"));
    }

    @Test
    void summarise_whenRetroOpen_returns409() throws Exception {
        // Arrange
        String message = PREFIX + "only a closed retrospective can be summarised";
        when(feedbackItemService.summarise(WebFixtures.RETRO_ID)).thenThrow(new RetroflowException(message));

        // Act & Assert
        mvc.perform(post(SUMMARY_URL)).andExpect(status().isConflict()).andExpect(errorBody(message, 409));
    }

    @Test
    void summarise_whenRetroUnknown_returns404() throws Exception {
        // Arrange
        when(feedbackItemService.summarise(WebFixtures.RETRO_ID))
                .thenThrow(new NotFoundException("Retrospective", WebFixtures.RETRO_ID));

        // Act & Assert
        mvc.perform(post(SUMMARY_URL)).andExpect(status().isNotFound()).andExpect(errorBody(PREFIX + "not found", 404));
    }

    @ParameterizedTest(name = "{0} gives HTTP {1}")
    @CsvSource({
            "TIMEOUT, 504, 'the summary took too long, try again later'",
            "RATE_LIMITED, 503, 'the summary service is busy, try again in a minute'",
            "NOT_CONFIGURED, 503, 'summaries are not available, the model is not configured'",
            "PROVIDER_ERROR, 502, 'the summary service failed, try again later'",
            "INVALID_ANSWER, 502, 'the summary service gave an answer we could not use, try again'"
    })
    void summarise_whenModelFails_returnsStatusForTheReason(Reason reason, int expectedStatus, String expectedReason)
            throws Exception {
        // Arrange
        when(feedbackItemService.summarise(WebFixtures.RETRO_ID))
                .thenThrow(new SummaryUnavailableException(WebFixtures.RETRO_ID, reason));

        // Act & Assert
        mvc.perform(post(SUMMARY_URL))
                .andExpect(status().is(expectedStatus))
                .andExpect(errorBody(PREFIX + expectedReason, expectedStatus));
    }

    @Test
    void summarise_whenRateLimited_tellsTheCallerWhenToRetry() throws Exception {
        // Arrange
        when(feedbackItemService.summarise(WebFixtures.RETRO_ID))
                .thenThrow(new SummaryUnavailableException(WebFixtures.RETRO_ID, Reason.RATE_LIMITED));

        // Act & Assert
        mvc.perform(post(SUMMARY_URL)).andExpect(status().isServiceUnavailable()).andExpect(header().string("Retry-After", "60"));
    }

}
