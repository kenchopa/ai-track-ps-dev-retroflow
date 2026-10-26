package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RetrospectiveControllerTest extends ControllerTestSupport {

    @Autowired
    RetrospectiveControllerTest(MockMvc mvc, TeamService teams, RetrospectiveService retrospectives) {
        super(mvc, teams, retrospectives);
    }

    @Test
    void createRetrospective_whenRequestIsValid_returnsCreatedResource() throws Exception {
        // Arrange
        when(retrospectives.createRetrospective(TEAM_ID, "Sprint retro", MEETING_DATE)).thenReturn(retrospective());

        // Act & Assert
        mvc.perform(post("/api/teams/{teamId}/retrospectives", TEAM_ID)
                .contentType(MediaType.APPLICATION_JSON).content(RETRO_JSON))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/api/retrospectives/" + RETRO_ID))
            .andExpect(jsonPath("$.teamId").value(TEAM_ID.intValue()))
            .andExpect(jsonPath("$.date").value(MEETING_DATE.toString()))
            .andExpect(jsonPath("$.status").value("OPEN"));
        verify(retrospectives).createRetrospective(TEAM_ID, "Sprint retro", MEETING_DATE);
    }

    @Test
    void createRetrospective_whenTeamAlreadyHasOpenRetro_returnsConflict() throws Exception {
        // Arrange
        var message = "Team " + TEAM_ID + ": already has an open retrospective";
        when(retrospectives.createRetrospective(TEAM_ID, "Sprint retro", MEETING_DATE))
            .thenThrow(new RetroflowException(message));

        // Act & Assert
        mvc.perform(post("/api/teams/{teamId}/retrospectives", TEAM_ID)
                .contentType(MediaType.APPLICATION_JSON).content(RETRO_JSON))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value(message))
            .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void createRetrospective_whenTeamIsMissing_returnsNotFound() throws Exception {
        // Arrange
        when(retrospectives.createRetrospective(TEAM_ID, "Sprint retro", MEETING_DATE))
            .thenThrow(new ResourceNotFoundException("Team", TEAM_ID));

        // Act & Assert
        mvc.perform(post("/api/teams/{teamId}/retrospectives", TEAM_ID)
                .contentType(MediaType.APPLICATION_JSON).content(RETRO_JSON))
            .andExpect(status().isNotFound());
    }

    @Test
    void listRetrospectives_whenHistoryExists_returnsArrayWithoutEntityGraph() throws Exception {
        // Arrange
        when(retrospectives.listRetrospectives(TEAM_ID)).thenReturn(List.of(retrospective()));

        // Act & Assert
        mvc.perform(get("/api/teams/{teamId}/retrospectives", TEAM_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value(RETRO_ID.intValue()))
            .andExpect(jsonPath("$[0].team").doesNotExist())
            .andExpect(jsonPath("$[0].feedbackItems").doesNotExist());
    }

    @Test
    void listRetrospectives_whenHistoryIsEmpty_returnsEmptyArray() throws Exception {
        // Arrange
        when(retrospectives.listRetrospectives(TEAM_ID)).thenReturn(List.of());

        // Act & Assert
        mvc.perform(get("/api/teams/{teamId}/retrospectives", TEAM_ID))
            .andExpect(status().isOk()).andExpect(content().json("[]"));
    }

    @Test
    void listRetrospectives_whenTeamIsMissing_returnsNotFound() throws Exception {
        // Arrange
        when(retrospectives.listRetrospectives(TEAM_ID)).thenThrow(new ResourceNotFoundException("Team", TEAM_ID));

        // Act & Assert
        mvc.perform(get("/api/teams/{teamId}/retrospectives", TEAM_ID))
            .andExpect(status().isNotFound());
    }

    @Test
    void closeRetrospective_whenSuccessful_returnsNoContent() throws Exception {
        // Arrange
        var request = put("/api/retrospectives/{id}/close", RETRO_ID);

        // Act & Assert
        mvc.perform(request).andExpect(status().isNoContent()).andExpect(content().string(""));
        verify(retrospectives).closeRetrospective(RETRO_ID);
    }

    @Test
    void closeRetrospective_whenRetroIsMissing_returnsNotFound() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("Retrospective", RETRO_ID))
            .when(retrospectives).closeRetrospective(RETRO_ID);

        // Act & Assert
        mvc.perform(put("/api/retrospectives/{id}/close", RETRO_ID))
            .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "{}", "{\"title\":\" \",\"date\":\"2026-10-26\"}", "{\"title\":\"Retro\"}",
        "{\"title\":\"Retro\",\"date\":\"not-a-date\"}", "{\"title\":\"Retro\",\"date\":\"2026-02-30\"}",
        "{\"title\":\"Retro\",\"date\":\"2026-10-26\",\"status\":\"CLOSED\"}",
        "{\"title\":\"Retro\",\"date\":[2026,10,26]}"
    })
    void createRetrospective_whenBodyIsInvalid_returnsBadRequestBeforeService(String body) throws Exception {
        // Arrange
        var request = post("/api/teams/{teamId}/retrospectives", TEAM_ID)
            .contentType(MediaType.APPLICATION_JSON).content(body);

        // Act & Assert
        mvc.perform(request).andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(teams, retrospectives);
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "not-a-number", "999999999999999999999999"})
    void retrospectiveEndpoints_whenIdIsInvalid_returnBadRequestBeforeService(String id) throws Exception {
        // Arrange
        var create = post("/api/teams/{teamId}/retrospectives", id)
            .contentType(MediaType.APPLICATION_JSON).content(RETRO_JSON);
        var list = get("/api/teams/{teamId}/retrospectives", id);
        var close = put("/api/retrospectives/{id}/close", id);

        // Act & Assert
        mvc.perform(create).andExpect(status().isBadRequest());
        mvc.perform(list).andExpect(status().isBadRequest());
        mvc.perform(close).andExpect(status().isBadRequest());
        verifyNoInteractions(teams, retrospectives);
    }

    @Test
    void closeRetrospective_whenServiceRejectsTransition_returnsConflict() throws Exception {
        // Arrange
        var message = "Retrospective " + RETRO_ID + ": requested transition is not allowed";
        doThrow(new RetroflowException(message)).when(retrospectives).closeRetrospective(RETRO_ID);

        // Act & Assert
        mvc.perform(put("/api/retrospectives/{id}/close", RETRO_ID))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value(message))
            .andExpect(jsonPath("$.status").value(409));
    }
}
