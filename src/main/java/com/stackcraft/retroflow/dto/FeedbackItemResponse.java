package com.stackcraft.retroflow.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.Priority;

/**
 * A feedback item or action item. {@code priority} and {@code completed} are
 * only present for action items (decisions.md F7).
 */
public record FeedbackItemResponse(
        Long id,
        Long retrospectiveId,
        FeedbackType type,
        String content,
        String author,
        @JsonInclude(JsonInclude.Include.NON_NULL) Priority priority,
        @JsonInclude(JsonInclude.Include.NON_NULL) Boolean completed) {

    public static FeedbackItemResponse from(FeedbackItem item) {
        Priority priority = null;
        Boolean completed = null;
        if (item instanceof ActionItem actionItem) {
            priority = actionItem.getPriority();
            completed = actionItem.isCompleted();
        }
        return new FeedbackItemResponse(item.getId(), item.getRetrospective().getId(), item.getType(),
                item.getContent(), item.getAuthor(), priority, completed);
    }

}
