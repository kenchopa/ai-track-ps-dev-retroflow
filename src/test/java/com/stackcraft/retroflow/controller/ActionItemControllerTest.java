package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;

import java.util.List;

import static com.stackcraft.retroflow.controller.WebFixtures.ACTION_CONTENT;
import static com.stackcraft.retroflow.controller.WebFixtures.ACTION_ID;
import static com.stackcraft.retroflow.controller.WebFixtures.ALICE;
import static com.stackcraft.retroflow.controller.WebFixtures.RETRO_ID;
import static com.stackcraft.retroflow.controller.WebFixtures.TEAM_ID;
import static com.stackcraft.retroflow.controller.WebFixtures.UNKNOWN_ID;
import static com.stackcraft.retroflow.controller.WebFixtures.actionItem;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
class ActionItemControllerTest extends ControllerContractTest {

    private static final String ADD_URL = "/api/retrospectives/" + RETRO_ID + "/action-items";
    private static final String ITEM_URL = "/api/action-items/" + ACTION_ID;
    private static final String FILTER_URL = "/api/teams/" + TEAM_ID + "/action-items";
    private static final String VALID_BODY = """
            {"content": "Fix the build", "priority": "HIGH", "author": "Alice"}""";
    private static final String UPDATE_BODY = """
            {"content": "Fix the build", "priority": "LOW"}""";

    @Test
    void addActionItem_withValidBody_returns201WithOpenActionItem() throws Exception {
        // Arrange
        when(actionItemService.addActionItem(RETRO_ID, ACTION_CONTENT, "HIGH", ALICE)).thenReturn(actionItem(false));

        // Act & Assert
        mvc.perform(post(ADD_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(ACTION_ID))
                .andExpect(jsonPath("$.type").value("ACTION_ITEM"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.completed").value(false));
    }

    @Test
    void addActionItem_withoutPriority_returns400() throws Exception {
        // Arrange
        String noPriority = """
                {"content": "Fix the build", "author": "Alice"}""";

        // Act & Assert
        mvc.perform(post(ADD_URL).contentType(MediaType.APPLICATION_JSON).content(noPriority))
                .andExpect(status().isBadRequest())
                .andExpect(errorBody("ActionItem new: priority must not be blank", 400));
        verifyNoInteractions(actionItemService);
    }

    @Test
    void addActionItem_whenRetroClosed_returns409() throws Exception {
        // Arrange
        String message = "Retrospective 10: cannot add feedback to a closed retrospective";
        when(actionItemService.addActionItem(RETRO_ID, ACTION_CONTENT, "HIGH", ALICE)).thenThrow(new RetroflowException(message));

        // Act & Assert
        mvc.perform(post(ADD_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(errorBody(message, 409));
    }

    @Test
    void updateActionItem_withValidBody_returns200() throws Exception {
        // Arrange
        when(actionItemService.updateActionItem(ACTION_ID, ACTION_CONTENT, "LOW")).thenReturn(actionItem(false));

        // Act & Assert
        mvc.perform(put(ITEM_URL).contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ACTION_ID));
    }

    @Test
    void updateActionItem_whenRetroClosed_returns409() throws Exception {
        // Arrange
        String message = "ActionItem 200: cannot modify an item of a closed retrospective";
        when(actionItemService.updateActionItem(ACTION_ID, ACTION_CONTENT, "LOW")).thenThrow(new RetroflowException(message));

        // Act & Assert
        mvc.perform(put(ITEM_URL).contentType(MediaType.APPLICATION_JSON).content(UPDATE_BODY))
                .andExpect(status().isConflict())
                .andExpect(errorBody(message, 409));
    }

    @Test
    void completeActionItem_whenOpen_returns200Completed() throws Exception {
        // Arrange
        when(actionItemService.completeActionItem(ACTION_ID)).thenReturn(actionItem(true));

        // Act & Assert
        mvc.perform(put(ITEM_URL + "/complete"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.completed").value(true));
    }

    @Test
    void completeActionItem_whenAlreadyCompleted_returns409() throws Exception {
        // Arrange
        String message = "ActionItem 200: already completed";
        when(actionItemService.completeActionItem(ACTION_ID)).thenThrow(new RetroflowException(message));

        // Act & Assert
        mvc.perform(put(ITEM_URL + "/complete"))
                .andExpect(status().isConflict())
                .andExpect(errorBody(message, 409));
    }

    @Test
    void uncompleteActionItem_whenCompleted_returns409() throws Exception {
        // Arrange
        String message = "ActionItem 200: already completed, cannot be undone";
        when(actionItemService.uncompleteActionItem(ACTION_ID)).thenThrow(new RetroflowException(message));

        // Act & Assert
        mvc.perform(put(ITEM_URL + "/uncomplete"))
                .andExpect(status().isConflict())
                .andExpect(errorBody(message, 409));
    }

    @Test
    void uncompleteActionItem_whenUnknown_returns404() throws Exception {
        // Arrange
        when(actionItemService.uncompleteActionItem(UNKNOWN_ID)).thenThrow(new NotFoundException("ActionItem", UNKNOWN_ID));

        // Act & Assert
        mvc.perform(put("/api/action-items/" + UNKNOWN_ID + "/uncomplete"))
                .andExpect(status().isNotFound())
                .andExpect(errorBody("ActionItem " + UNKNOWN_ID + ": not found", 404));
    }

    @Test
    void getActionItemsForTeam_withFilters_returns200WithMatches() throws Exception {
        // Arrange
        when(actionItemService.getActionItemsForTeam(TEAM_ID, "HIGH", false)).thenReturn(List.of(actionItem(false)));

        // Act & Assert
        mvc.perform(get(FILTER_URL).param("priority", "HIGH").param("completed", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(ACTION_ID));
    }

    @Test
    void getActionItemsForTeam_withUnknownPriority_returns400() throws Exception {
        // Arrange
        String message = "Team 1: priority filter must be one of LOW, MEDIUM, HIGH";
        when(actionItemService.getActionItemsForTeam(TEAM_ID, "URGENT", null)).thenThrow(new InvalidInputException(message));

        // Act & Assert
        mvc.perform(get(FILTER_URL).param("priority", "URGENT"))
                .andExpect(status().isBadRequest())
                .andExpect(errorBody(message, 400));
    }

    @Test
    void getActionItemsForTeam_withNonBooleanCompleted_returns400() throws Exception {
        // Act & Assert
        mvc.perform(get(FILTER_URL).param("completed", "maybe"))
                .andExpect(status().isBadRequest())
                .andExpect(errorBody("Request -: invalid value for 'completed'", 400));
    }

}
