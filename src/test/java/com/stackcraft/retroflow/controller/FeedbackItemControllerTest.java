package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;

import java.util.List;

import static com.stackcraft.retroflow.controller.WebFixtures.ALICE;
import static com.stackcraft.retroflow.controller.WebFixtures.CONTENT;
import static com.stackcraft.retroflow.controller.WebFixtures.FEEDBACK_ID;
import static com.stackcraft.retroflow.controller.WebFixtures.RETRO_ID;
import static com.stackcraft.retroflow.controller.WebFixtures.UNKNOWN_ID;
import static com.stackcraft.retroflow.controller.WebFixtures.actionItem;
import static com.stackcraft.retroflow.controller.WebFixtures.feedbackItem;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
class FeedbackItemControllerTest extends ControllerContractTest {

    private static final String ITEMS_URL = "/api/retrospectives/" + RETRO_ID + "/feedback-items";
    private static final String ITEM_URL = "/api/feedback-items/" + FEEDBACK_ID;
    private static final String VALID_BODY = """
            {"content": "Pairing worked well", "type": "WENT_WELL", "author": "Alice"}""";
    private static final String CHANGED = "Pairing worked really well";

    @Test
    void addFeedbackItem_withValidBody_returns201WithItem() throws Exception {
        // Arrange
        when(feedbackItemService.addFeedbackItem(RETRO_ID, CONTENT, "WENT_WELL", ALICE)).thenReturn(feedbackItem(CONTENT));

        // Act & Assert
        mvc.perform(post(ITEMS_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(FEEDBACK_ID))
                .andExpect(jsonPath("$.retrospectiveId").value(RETRO_ID))
                .andExpect(jsonPath("$.type").value("WENT_WELL"))
                .andExpect(jsonPath("$.author").value(ALICE))
                .andExpect(jsonPath("$.priority").doesNotExist());
    }

    @Test
    void addFeedbackItem_withEmptyBody_returns400WithEveryReason() throws Exception {
        // Act & Assert
        mvc.perform(post(ITEMS_URL).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(errorBody("FeedbackItem new: author must not be blank; "
                        + "content must not be blank; type must not be blank", 400));
        verifyNoInteractions(feedbackItemService);
    }

    @Test
    void addFeedbackItem_whenRetroClosed_returns409() throws Exception {
        // Arrange
        String message = "Retrospective 10: cannot add feedback to a closed retrospective";
        when(feedbackItemService.addFeedbackItem(RETRO_ID, CONTENT, "WENT_WELL", ALICE))
                .thenThrow(new RetroflowException(message));

        // Act & Assert
        mvc.perform(post(ITEMS_URL).contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(errorBody(message, 409));
    }

    @Test
    void getFeedbackItems_returns200WithActionItemFields() throws Exception {
        // Arrange
        when(feedbackItemService.getFeedbackItems(RETRO_ID)).thenReturn(List.of(feedbackItem(CONTENT), actionItem(false)));

        // Act & Assert
        mvc.perform(get(ITEMS_URL))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].priority").doesNotExist())
                .andExpect(jsonPath("$[1].type").value("ACTION_ITEM"))
                .andExpect(jsonPath("$[1].priority").value("HIGH"))
                .andExpect(jsonPath("$[1].completed").value(false));
    }

    @Test
    void getFeedbackItems_forUnknownRetro_returns404() throws Exception {
        // Arrange
        when(feedbackItemService.getFeedbackItems(UNKNOWN_ID)).thenThrow(new NotFoundException("Retrospective", UNKNOWN_ID));

        // Act & Assert
        mvc.perform(get("/api/retrospectives/" + UNKNOWN_ID + "/feedback-items"))
                .andExpect(status().isNotFound())
                .andExpect(errorBody("Retrospective " + UNKNOWN_ID + ": not found", 404));
    }

    @Test
    void updateFeedbackItem_withValidBody_returns200WithNewContent() throws Exception {
        // Arrange
        when(feedbackItemService.updateFeedbackItem(FEEDBACK_ID, CHANGED)).thenReturn(feedbackItem(CHANGED));

        // Act & Assert
        mvc.perform(put(ITEM_URL).contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"" + CHANGED + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").value(CHANGED));
    }

    @Test
    void updateFeedbackItem_withBlankContent_returns400NamingTheItem() throws Exception {
        // Act & Assert
        mvc.perform(put(ITEM_URL).contentType(MediaType.APPLICATION_JSON).content("{\"content\": \" \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(errorBody("FeedbackItem " + FEEDBACK_ID + ": content must not be blank", 400));
    }

    @Test
    void updateFeedbackItem_whenRetroClosed_returns409() throws Exception {
        // Arrange
        String message = "FeedbackItem 100: cannot modify an item of a closed retrospective";
        when(feedbackItemService.updateFeedbackItem(FEEDBACK_ID, CHANGED)).thenThrow(new RetroflowException(message));

        // Act & Assert
        mvc.perform(put(ITEM_URL).contentType(MediaType.APPLICATION_JSON).content("{\"content\": \"" + CHANGED + "\"}"))
                .andExpect(status().isConflict())
                .andExpect(errorBody(message, 409));
    }

    @Test
    void deleteFeedbackItem_whenRetroOpen_returns204() throws Exception {
        // Act & Assert
        mvc.perform(delete(ITEM_URL)).andExpect(status().isNoContent());
        verify(feedbackItemService).deleteFeedbackItem(FEEDBACK_ID);
    }

    @Test
    void deleteFeedbackItem_whenRetroClosed_returns409() throws Exception {
        // Arrange
        String message = "FeedbackItem 100: cannot delete an item of a closed retrospective";
        doThrow(new RetroflowException(message)).when(feedbackItemService).deleteFeedbackItem(FEEDBACK_ID);

        // Act & Assert
        mvc.perform(delete(ITEM_URL))
                .andExpect(status().isConflict())
                .andExpect(errorBody(message, 409));
    }

    @Test
    void deleteFeedbackItem_whenUnknown_returns404() throws Exception {
        // Arrange
        doThrow(new NotFoundException("FeedbackItem", UNKNOWN_ID)).when(feedbackItemService).deleteFeedbackItem(UNKNOWN_ID);

        // Act & Assert
        mvc.perform(delete("/api/feedback-items/" + UNKNOWN_ID))
                .andExpect(status().isNotFound())
                .andExpect(errorBody("FeedbackItem " + UNKNOWN_ID + ": not found", 404));
    }

}
