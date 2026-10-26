package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.service.ActionItemService;
import com.stackcraft.retroflow.service.FeedbackItemService;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

/**
 * Shared setup for the web-layer contract tests: every controller is loaded
 * by {@code @WebMvcTest}, so every service is mocked. Tests talk JSON only,
 * so they survive refactoring of the classes behind the API.
 */
abstract class ControllerContractTest {

    @Autowired
    protected MockMvc mvc;

    @MockBean
    protected TeamService teamService;

    @MockBean
    protected RetrospectiveService retrospectiveService;

    @MockBean
    protected FeedbackItemService feedbackItemService;

    @MockBean
    protected ActionItemService actionItemService;

    /** The StackCraft error body (standard 2.2) with this message and status. */
    protected static ResultMatcher errorBody(String message, int status) {
        return result -> {
            jsonPath("$.error").value(message).match(result);
            jsonPath("$.status").value(status).match(result);
            jsonPath("$.timestamp").isNotEmpty().match(result);
        };
    }

}
