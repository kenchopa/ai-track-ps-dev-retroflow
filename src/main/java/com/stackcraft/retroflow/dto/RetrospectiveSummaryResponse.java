package com.stackcraft.retroflow.dto;

import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.service.RetrospectiveSummary;

import java.util.List;

/**
 * Body of {@code POST /api/retrospectives/{id}/summary}: a summary written by a
 * language model and validated against the retrospective's feedback.
 */
public record RetrospectiveSummaryResponse(List<String> wentWell,
                                           List<String> needsImprovement,
                                           List<OpenActionItem> openActionItems) {

    public record OpenActionItem(Long id, String summary, Priority priority) {
    }

    public static RetrospectiveSummaryResponse from(RetrospectiveSummary summary) {
        return new RetrospectiveSummaryResponse(summary.wentWell(), summary.needsImprovement(),
                summary.openActionItems().stream()
                        .map(item -> new OpenActionItem(item.id(), item.summary(), item.priority()))
                        .toList());
    }

}
