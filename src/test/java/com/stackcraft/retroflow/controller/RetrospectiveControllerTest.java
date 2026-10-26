package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;

import java.util.List;

import static com.stackcraft.retroflow.controller.WebFixtures.RETRO_DATE;
import static com.stackcraft.retroflow.controller.WebFixtures.RETRO_ID;
import static com.stackcraft.retroflow.controller.WebFixtures.RETRO_TITLE;
import static com.stackcraft.retroflow.controller.WebFixtures.TEAM_ID;
import static com.stackcraft.retroflow.controller.WebFixtures.UNKNOWN_ID;
import static com.stackcraft.retroflow.controller.WebFixtures.closedRetro;
import static com.stackcraft.retroflow.controller.WebFixtures.retro;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
class RetrospectiveControllerTest extends ControllerContractTest {

    private static final String CREATE_URL = "/api/teams/" + TEAM_ID + "/retrospectives";
    private static final String VALID_BODY = """
            {"title": "Sprint 1 Retro", "date": "2026-10-26"}""";

    @Test
    void createRetrospective_withValidBody_returns201WithOpenRetro() throws Exception {
        // Arrange
        when(retrospectiveService.createRetrospective(TEAM_ID, RETRO_TITLE, RETRO_DATE)).thenReturn(retro());

        // Act & Assert
        mvc.perform(post(CREATE_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/retrospectives/" + RETRO_ID))
                .andExpect(jsonPath("$.id").value(RETRO_ID))
                .andExpect(jsonPath("$.teamId").value(TEAM_ID))
                .andExpect(jsonPath("$.date").value("2026-10-26"))
                .andExpect(jsonPath("$.status").value("OPEN"));
    }

    @Test
    void createRetrospective_withBlankTitle_returns400() throws Exception {
        // Act & Assert
        mvc.perform(post(CREATE_URL).contentType(MediaType.APPLICATION_JSON).content("{\"title\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(errorBody("Retrospective new: title must not be blank", 400));
        verifyNoInteractions(retrospectiveService);
    }

    @Test
    void createRetrospective_withInvalidDate_returns400() throws Exception {
        // Arrange
        String europeanDate = """
                {"title": "Sprint 1 Retro", "date": "26-10-2026"}""";

        // Act & Assert
        mvc.perform(post(CREATE_URL).contentType(MediaType.APPLICATION_JSON).content(europeanDate))
                .andExpect(status().isBadRequest())
                .andExpect(errorBody("Request -: malformed JSON or invalid field value", 400));
    }

    @Test
    void createRetrospective_forUnknownTeam_returns404() throws Exception {
        // Arrange
        when(retrospectiveService.createRetrospective(TEAM_ID, RETRO_TITLE, RETRO_DATE))
                .thenThrow(new NotFoundException("Team", TEAM_ID));

        // Act & Assert
        mvc.perform(post(CREATE_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isNotFound())
                .andExpect(errorBody("Team " + TEAM_ID + ": not found", 404));
    }

    @Test
    void createRetrospective_whenTeamHasOpenRetro_returns409() throws Exception {
        // Arrange
        String message = "Team 1: already has an open retrospective (10)";
        when(retrospectiveService.createRetrospective(TEAM_ID, RETRO_TITLE, RETRO_DATE))
                .thenThrow(new RetroflowException(message));

        // Act & Assert
        mvc.perform(post(CREATE_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(errorBody(message, 409));
    }

    @Test
    void getRetrospectivesForTeam_returns200WithArray() throws Exception {
        // Arrange
        when(retrospectiveService.getRetrospectivesForTeam(TEAM_ID)).thenReturn(List.of(retro()));

        // Act & Assert
        mvc.perform(get(CREATE_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value(RETRO_TITLE));
    }

    @Test
    void getRetrospective_whenUnknown_returns404() throws Exception {
        // Arrange
        when(retrospectiveService.getRetrospective(UNKNOWN_ID))
                .thenThrow(new NotFoundException("Retrospective", UNKNOWN_ID));

        // Act & Assert
        mvc.perform(get("/api/retrospectives/" + UNKNOWN_ID))
                .andExpect(status().isNotFound())
                .andExpect(errorBody("Retrospective " + UNKNOWN_ID + ": not found", 404));
    }

    @Test
    void closeRetrospective_whenOpen_returns200WithClosedRetro() throws Exception {
        // Arrange
        when(retrospectiveService.closeRetrospective(RETRO_ID)).thenReturn(closedRetro());

        // Act & Assert
        mvc.perform(put("/api/retrospectives/" + RETRO_ID + "/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void closeRetrospective_whenAlreadyClosed_returns409() throws Exception {
        // Arrange
        String message = "Retrospective 10: already closed";
        when(retrospectiveService.closeRetrospective(RETRO_ID)).thenThrow(new RetroflowException(message));

        // Act & Assert
        mvc.perform(put("/api/retrospectives/" + RETRO_ID + "/close"))
                .andExpect(status().isConflict())
                .andExpect(errorBody(message, 409));
    }

    @Test
    void reopenRetrospective_hasNoEndpoint_returns404() throws Exception {
        // Act & Assert (test-matrix.md row 5)
        mvc.perform(put("/api/retrospectives/" + RETRO_ID + "/reopen"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

}
