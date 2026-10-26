package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;

import java.util.List;

import static com.stackcraft.retroflow.controller.WebFixtures.ALICE;
import static com.stackcraft.retroflow.controller.WebFixtures.MEMBERS;
import static com.stackcraft.retroflow.controller.WebFixtures.TEAM_ID;
import static com.stackcraft.retroflow.controller.WebFixtures.TEAM_NAME;
import static com.stackcraft.retroflow.controller.WebFixtures.UNKNOWN_ID;
import static com.stackcraft.retroflow.controller.WebFixtures.team;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
class TeamControllerTest extends ControllerContractTest {

    private static final String VALID_BODY = """
            {"name": "Team Alpha", "members": ["Alice", "Bob"]}""";

    @Test
    void createTeam_withValidBody_returns201WithLocationAndTeam() throws Exception {
        // Arrange
        when(teamService.createTeam(TEAM_NAME, MEMBERS)).thenReturn(team());

        // Act & Assert
        mvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", "/api/teams/" + TEAM_ID))
                .andExpect(jsonPath("$.id").value(TEAM_ID))
                .andExpect(jsonPath("$.name").value(TEAM_NAME))
                .andExpect(jsonPath("$.members[0]").value(ALICE));
    }

    @Test
    void createTeam_withBlankNameAndNoMembers_returns400WithEveryReason() throws Exception {
        // Arrange
        String invalidBody = """
                {"name": " ", "members": []}""";

        // Act & Assert
        mvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON).content(invalidBody))
                .andExpect(status().isBadRequest())
                .andExpect(errorBody("Team new: members must not be empty; name must not be blank", 400));
        verifyNoInteractions(teamService);
    }

    @Test
    void createTeam_withMalformedJson_returns400() throws Exception {
        // Act & Assert
        mvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON).content("{\"name\": "))
                .andExpect(status().isBadRequest())
                .andExpect(errorBody("Request -: malformed JSON or invalid field value", 400));
    }

    @Test
    void createTeam_whenNameTaken_returns409() throws Exception {
        // Arrange
        String message = "Team new: a team named 'Team Alpha' already exists";
        when(teamService.createTeam(TEAM_NAME, MEMBERS)).thenThrow(new RetroflowException(message));

        // Act & Assert
        mvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(errorBody(message, 409));
    }

    @Test
    void createTeam_whenDatabaseRejectsDuplicate_returns409WithoutDatabaseDetails() throws Exception {
        // Arrange
        String databaseMessage = "could not execute statement; constraint [UK_TEAM_NAME]";
        when(teamService.createTeam(TEAM_NAME, MEMBERS)).thenThrow(new DataIntegrityViolationException(databaseMessage));

        // Act & Assert
        mvc.perform(post("/api/teams").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value(not(containsString("constraint"))))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void getAllTeams_returns200WithArray() throws Exception {
        // Arrange
        when(teamService.getAllTeams()).thenReturn(List.of(team()));

        // Act & Assert
        mvc.perform(get("/api/teams"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(TEAM_ID))
                .andExpect(jsonPath("$[0].name").value(TEAM_NAME));
    }

    @Test
    void getTeam_whenExists_returns200() throws Exception {
        // Arrange
        when(teamService.getTeam(TEAM_ID)).thenReturn(team());

        // Act & Assert
        mvc.perform(get("/api/teams/" + TEAM_ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.members.length()").value(MEMBERS.size()));
    }

    @Test
    void getTeam_whenUnknown_returns404() throws Exception {
        // Arrange
        when(teamService.getTeam(UNKNOWN_ID)).thenThrow(new NotFoundException("Team", UNKNOWN_ID));

        // Act & Assert
        mvc.perform(get("/api/teams/" + UNKNOWN_ID))
                .andExpect(status().isNotFound())
                .andExpect(errorBody("Team " + UNKNOWN_ID + ": not found", 404));
    }

    @Test
    void getTeam_withNonNumericId_returns400() throws Exception {
        // Act & Assert
        mvc.perform(get("/api/teams/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(errorBody("Request -: invalid value for 'id'", 400));
    }

}
