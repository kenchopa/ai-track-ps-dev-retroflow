package com.stackcraft.retroflow.dto;

import com.stackcraft.retroflow.entity.FeedbackItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/retrospectives/{id}/feedback-items}.
 *
 * @param type WENT_WELL or NEEDS_IMPROVEMENT; checked by the service so the error lists the allowed values
 */
public record CreateFeedbackItemRequest(
        @NotBlank @Size(max = FeedbackItem.MAX_CONTENT_LENGTH) String content,
        @NotBlank String type,
        @NotBlank @Size(max = FeedbackItem.MAX_AUTHOR_LENGTH) String author) {
}
