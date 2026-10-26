package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Priority;

import java.util.List;

/**
 * A validated summary of a closed retrospective.
 *
 * @param wentWell         what went well, in a few short points
 * @param needsImprovement what needs improvement, in a few short points
 * @param openActionItems  every action item that is not completed yet
 */
public record RetrospectiveSummary(List<String> wentWell,
                                   List<String> needsImprovement,
                                   List<OpenActionItem> openActionItems) {

    /** An open action item; id and priority are checked against the database. */
    public record OpenActionItem(Long id, String summary, Priority priority) {
    }

}
