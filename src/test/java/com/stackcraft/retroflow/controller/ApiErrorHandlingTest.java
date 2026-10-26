package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ApiErrorHandlingTest extends ControllerTestSupport {

    @Autowired
    ApiErrorHandlingTest(MockMvc mvc, TeamService teams, RetrospectiveService retrospectives) {
        super(mvc, teams, retrospectives);
    }

    @Test
    void getTeam_whenUnexpectedFailureOccurs_returnsSanitizedServerError() throws Exception {
        // Arrange
        when(teams.getTeam(TEAM_ID)).thenThrow(new IllegalStateException("database credentials or internals"));

        // Act & Assert
        mvc.perform(get("/api/teams/{id}", TEAM_ID))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Request: an unexpected server error occurred"))
            .andExpect(jsonPath("$.status").value(500))
            .andExpect(jsonPath("$.timestamp").isString())
            .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void createTeam_whenServiceIsStillStubbed_returnsServerErrorInsteadOfSuccess() throws Exception {
        // Arrange
        when(teams.createTeam(anyString(), anyList())).thenCallRealMethod();

        // Act & Assert
        mvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON).content(TEAM_JSON))
            .andExpect(status().isInternalServerError())
            .andExpect(jsonPath("$.error").value("Request: an unexpected server error occurred"));
    }

    @Test
    void request_whenMethodIsUnsupported_preservesMethodNotAllowed() throws Exception {
        // Arrange
        var request = delete("/api/teams/{id}", TEAM_ID);

        // Act & Assert
        mvc.perform(request).andExpect(status().isMethodNotAllowed())
            .andExpect(jsonPath("$.status").value(405));
        verifyNoInteractions(teams, retrospectives);
    }

    @Test
    void createTeam_whenMediaTypeIsUnsupported_preservesUnsupportedMediaType() throws Exception {
        // Arrange
        var request = post("/api/teams").contentType(MediaType.TEXT_PLAIN).content(TEAM_JSON);

        // Act & Assert
        mvc.perform(request).andExpect(status().isUnsupportedMediaType())
            .andExpect(jsonPath("$.status").value(415));
        verifyNoInteractions(teams, retrospectives);
    }
}
