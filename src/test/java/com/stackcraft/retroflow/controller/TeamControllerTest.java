package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.stream.Stream;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class TeamControllerTest extends ControllerTestSupport {

    private static final int OVERLONG_NAME_LENGTH = 201;

    @Autowired
    TeamControllerTest(MockMvc mvc, TeamService teams, RetrospectiveService retrospectives) {
        super(mvc, teams, retrospectives);
    }

    @Test
    void createTeam_whenRequestIsValid_returnsCreatedResourceAndLocation() throws Exception {
        // Arrange
        when(teams.createTeam("Alpha", List.of("Alice"))).thenReturn(team());

        // Act & Assert
        mvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON).content(TEAM_JSON))
            .andExpect(status().isCreated())
            .andExpect(header().string("Location", "http://localhost/api/teams/" + TEAM_ID))
            .andExpect(jsonPath("$.id").value(TEAM_ID.intValue()))
            .andExpect(jsonPath("$.name").value("Alpha"))
            .andExpect(jsonPath("$.members[0]").value("Alice"))
            .andExpect(jsonPath("$.retrospectives").doesNotExist());
        verify(teams).createTeam("Alpha", List.of("Alice"));
    }

    @Test
    void getTeam_whenTeamExists_returnsTeam() throws Exception {
        // Arrange
        when(teams.getTeam(TEAM_ID)).thenReturn(team());

        // Act & Assert
        mvc.perform(get("/api/teams/{id}", TEAM_ID))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(TEAM_ID.intValue()))
            .andExpect(jsonPath("$.members[0]").value("Alice"));
    }

    @Test
    void getTeam_whenTeamDoesNotExist_returnsStructuredNotFound() throws Exception {
        // Arrange
        when(teams.getTeam(TEAM_ID)).thenThrow(new ResourceNotFoundException("Team", TEAM_ID));

        // Act & Assert
        mvc.perform(get("/api/teams/{id}", TEAM_ID))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.error").value("Team " + TEAM_ID + ": not found"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.timestamp").isString());
    }

    @ParameterizedTest
    @MethodSource("invalidTeamBodies")
    void createTeam_whenBodyIsInvalid_returnsBadRequestBeforeService(String body) throws Exception {
        // Arrange
        var request = post("/api/teams").contentType(MediaType.APPLICATION_JSON).content(body);

        // Act & Assert
        mvc.perform(request).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").isNotEmpty())
            .andExpect(jsonPath("$.status").value(400));
        verifyNoInteractions(teams, retrospectives);
    }

    static Stream<String> invalidTeamBodies() {
        var overlongName = "x".repeat(OVERLONG_NAME_LENGTH);
        return Stream.of("", "null", "{", "{}", """
            {"name":" ","members":["Alice"]}
            """, """
            {"name":"Alpha","members":[]}
            """, """
            {"name":"Alpha","members":[null]}
            """, """
            {"name":"Alpha","members":[" "]}
            """, "{\"name\":\"" + overlongName + "\",\"members\":[\"Alice\"]}",
            "{\"name\":\"Alpha\",\"members\":[\"" + overlongName + "\"]}",
            "{\"name\":123,\"members\":[\"Alice\"]}",
            "{\"name\":\"Alpha\",\"members\":[true]}",
            "{\"name\":\"Alpha\",\"members\":[\"Alice\"],\"id\":7}",
            TEAM_JSON + "{}");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-1", "not-a-number", "999999999999999999999999"})
    void getTeam_whenIdIsInvalid_returnsBadRequestBeforeService(String id) throws Exception {
        // Arrange
        var request = get("/api/teams/{id}", id);

        // Act & Assert
        mvc.perform(request).andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.error").value(containsString("id")));
        verifyNoInteractions(teams, retrospectives);
    }
}
