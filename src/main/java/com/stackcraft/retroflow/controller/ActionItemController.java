package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.CreateActionItemRequest;
import com.stackcraft.retroflow.dto.FeedbackItemResponse;
import com.stackcraft.retroflow.dto.UpdateActionItemRequest;
import com.stackcraft.retroflow.service.ActionItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class ActionItemController {

    private final ActionItemService actionItemService;

    public ActionItemController(ActionItemService actionItemService) {
        this.actionItemService = actionItemService;
    }

    /** 201 Created, 400 invalid input or author not in team, 404 unknown retro, 409 retro closed. */
    @PostMapping("/retrospectives/{retrospectiveId}/action-items")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackItemResponse addActionItem(@PathVariable Long retrospectiveId,
                                              @Valid @RequestBody CreateActionItemRequest request) {
        return FeedbackItemResponse.from(actionItemService.addActionItem(
                retrospectiveId, request.content(), request.priority(), request.author()));
    }

    /** 200 OK, 400 invalid input, 404 unknown action item, 409 retro closed. */
    @PutMapping("/action-items/{id}")
    public FeedbackItemResponse updateActionItem(@PathVariable Long id,
                                                 @Valid @RequestBody UpdateActionItemRequest request) {
        return FeedbackItemResponse.from(actionItemService.updateActionItem(id, request.content(), request.priority()));
    }

    /** 200 OK, 404 unknown action item, 409 already completed. */
    @PutMapping("/action-items/{id}/complete")
    public FeedbackItemResponse completeActionItem(@PathVariable Long id) {
        return FeedbackItemResponse.from(actionItemService.completeActionItem(id));
    }

    /** 200 OK (no change) if not completed, 404 unknown action item, 409 if completed. */
    @PutMapping("/action-items/{id}/uncomplete")
    public FeedbackItemResponse uncompleteActionItem(@PathVariable Long id) {
        return FeedbackItemResponse.from(actionItemService.uncompleteActionItem(id));
    }

    /** 200 OK, 400 unknown priority or non-boolean completed, 404 unknown team. */
    @GetMapping("/teams/{teamId}/action-items")
    public List<FeedbackItemResponse> getActionItemsForTeam(@PathVariable Long teamId,
                                                            @RequestParam(required = false) String priority,
                                                            @RequestParam(required = false) Boolean completed) {
        return actionItemService.getActionItemsForTeam(teamId, priority, completed).stream()
                .map(FeedbackItemResponse::from)
                .toList();
    }

}
