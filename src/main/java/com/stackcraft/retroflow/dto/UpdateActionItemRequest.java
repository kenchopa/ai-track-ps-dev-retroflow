package com.stackcraft.retroflow.dto;

import com.stackcraft.retroflow.entity.FeedbackItem;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Body of {@code PUT /api/action-items/{id}}: content and priority can change (decisions.md F3).
 */
public record UpdateActionItemRequest(
        @NotBlank @Size(max = FeedbackItem.MAX_CONTENT_LENGTH) String content,
        @NotBlank String priority) {
}
