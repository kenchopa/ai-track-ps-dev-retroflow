package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.dto.CreateFeedbackItemRequest;
import com.stackcraft.retroflow.dto.FeedbackItemResponse;
import com.stackcraft.retroflow.dto.RetrospectiveSummaryResponse;
import com.stackcraft.retroflow.dto.UpdateFeedbackItemRequest;
import com.stackcraft.retroflow.service.FeedbackItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
public class FeedbackItemController {

    private final FeedbackItemService feedbackItemService;

    public FeedbackItemController(FeedbackItemService feedbackItemService) {
        this.feedbackItemService = feedbackItemService;
    }

    /** 201 Created, 400 invalid input or author not in team, 404 unknown retro, 409 retro closed. */
    @PostMapping("/retrospectives/{retrospectiveId}/feedback-items")
    @ResponseStatus(HttpStatus.CREATED)
    public FeedbackItemResponse addFeedbackItem(@PathVariable Long retrospectiveId,
                                                @Valid @RequestBody CreateFeedbackItemRequest request) {
        return FeedbackItemResponse.from(feedbackItemService.addFeedbackItem(
                retrospectiveId, request.content(), request.type(), request.author()));
    }

    /** 200 OK with all items, action items included; 404 unknown retro. */
    @GetMapping("/retrospectives/{retrospectiveId}/feedback-items")
    public List<FeedbackItemResponse> getFeedbackItems(@PathVariable Long retrospectiveId) {
        return feedbackItemService.getFeedbackItems(retrospectiveId).stream()
                .map(FeedbackItemResponse::from)
                .toList();
    }

    /**
     * 200 OK with a summary written by a language model. 404 unknown retro, 409 retro still open,
     * 400 too many items, 502/503/504 when the model fails (the message says which).
     */
    @PostMapping("/retrospectives/{retrospectiveId}/summary")
    public RetrospectiveSummaryResponse summarise(@PathVariable Long retrospectiveId) {
        return RetrospectiveSummaryResponse.from(feedbackItemService.summarise(retrospectiveId));
    }

    /** 200 OK, 400 invalid content, 404 unknown item, 409 retro closed. */
    @PutMapping("/feedback-items/{id}")
    public FeedbackItemResponse updateFeedbackItem(@PathVariable Long id,
                                                   @Valid @RequestBody UpdateFeedbackItemRequest request) {
        return FeedbackItemResponse.from(feedbackItemService.updateFeedbackItem(id, request.content()));
    }

    /** 204 No Content, 404 unknown item, 409 retro closed. */
    @DeleteMapping("/feedback-items/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFeedbackItem(@PathVariable Long id) {
        feedbackItemService.deleteFeedbackItem(id);
    }

}
