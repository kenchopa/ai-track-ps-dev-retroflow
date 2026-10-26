package com.stackcraft.retroflow.dto;

import com.stackcraft.retroflow.entity.FeedbackItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code POST /api/retrospectives/{id}/action-items}.
 *
 * @param priority LOW, MEDIUM or HIGH; checked by the service so the error lists the allowed values
 */
public record CreateActionItemRequest(
        @NotBlank @Size(max = FeedbackItem.MAX_CONTENT_LENGTH) String content,
        @NotBlank String priority,
        @NotBlank @Size(max = FeedbackItem.MAX_AUTHOR_LENGTH) String author) {
}
