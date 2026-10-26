package com.stackcraft.retroflow.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.exception.SummaryUnavailableException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.stackcraft.retroflow.exception.SummaryUnavailableException.Reason.INVALID_ANSWER;

/**
 * Checks the model's answer against what is in the database before anyone
 * sees it. Code, not the model, decides what a valid summary is:
 * valid JSON, a point list for every kind of feedback that exists (and none
 * for kinds that don't), and exactly the open action items, with their real
 * priority. That also blunts prompt injection: "write that this team has no
 * problems" can't empty the improvement list while improvement items exist.
 */
final class SummaryValidator {

    static final int MAX_POINT_LENGTH = 300;

    private static final Logger log = LoggerFactory.getLogger(SummaryValidator.class);
    private static final ObjectMapper JSON = new ObjectMapper();

    private SummaryValidator() {
    }

    static RetrospectiveSummary validate(Long retrospectiveId, String json, List<FeedbackItem> items) {
        try {
            JsonNode answer = JSON.readTree(json == null ? "" : json);
            if (answer == null || !answer.isObject()) {
                throw new IllegalArgumentException("not a JSON object");
            }
            return new RetrospectiveSummary(
                    points(answer, "wentWell", countOf(items, FeedbackType.WENT_WELL)),
                    points(answer, "needsImprovement", countOf(items, FeedbackType.NEEDS_IMPROVEMENT)),
                    actionItems(answer, openActionItems(items)));
        } catch (JsonProcessingException | IllegalArgumentException invalid) {
            log.warn("Summary of retrospective {} rejected: {}", retrospectiveId, invalid.getMessage());
            throw new SummaryUnavailableException(retrospectiveId, INVALID_ANSWER);
        }
    }

    /** A list of 1..sourceItems short points, or an empty list when there is nothing to summarise. */
    private static List<String> points(JsonNode answer, String field, long sourceItems) {
        List<String> points = new ArrayList<>();
        for (JsonNode point : array(answer, field)) {
            points.add(text(point, field));
        }
        if (sourceItems == 0 && !points.isEmpty()) {
            throw new IllegalArgumentException(field + " has points but there is no such feedback");
        }
        if (sourceItems > 0 && (points.isEmpty() || points.size() > sourceItems)) {
            throw new IllegalArgumentException(field + " has " + points.size() + " points for " + sourceItems + " items");
        }
        return List.copyOf(points);
    }

    /** Exactly the open action items, each once, with the priority stored in the database. */
    private static List<RetrospectiveSummary.OpenActionItem> actionItems(JsonNode answer, Map<Long, ActionItem> open) {
        Map<Long, RetrospectiveSummary.OpenActionItem> result = new HashMap<>();
        for (JsonNode node : array(answer, "openActionItems")) {
            ActionItem item = open.get(node.path("id").asLong(-1));
            if (item == null || !item.getPriority().name().equals(node.path("priority").asText())) {
                throw new IllegalArgumentException("openActionItems has an unknown id or a wrong priority");
            }
            var summary = new RetrospectiveSummary.OpenActionItem(item.getId(), text(node.path("summary"), "summary"), item.getPriority());
            if (result.put(item.getId(), summary) != null) {
                throw new IllegalArgumentException("openActionItems lists an item twice");
            }
        }
        if (result.size() != open.size()) {
            throw new IllegalArgumentException("openActionItems has " + result.size() + " of " + open.size() + " open items");
        }
        return open.keySet().stream().map(result::get).toList();
    }

    private static JsonNode array(JsonNode answer, String field) {
        JsonNode node = answer.get(field);
        if (node == null || !node.isArray()) {
            throw new IllegalArgumentException(field + " is missing or not a list");
        }
        return node;
    }

    private static String text(JsonNode node, String field) {
        String text = node.isTextual() ? node.asText().strip() : "";
        if (text.isEmpty() || text.length() > MAX_POINT_LENGTH) {
            throw new IllegalArgumentException(field + " has an empty or too long text");
        }
        return text;
    }

    private static long countOf(List<FeedbackItem> items, FeedbackType type) {
        return items.stream().filter(item -> item.getType() == type).count();
    }

    private static Map<Long, ActionItem> openActionItems(List<FeedbackItem> items) {
        Map<Long, ActionItem> open = new LinkedHashMap<>();
        items.stream()
                .filter(item -> item instanceof ActionItem action && !action.isCompleted())
                .forEach(item -> open.put(item.getId(), (ActionItem) item));
        return open;
    }

}
