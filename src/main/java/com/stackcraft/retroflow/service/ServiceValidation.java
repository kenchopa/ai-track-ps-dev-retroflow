package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.RetroflowException;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Checks shared by the services. Every message follows StackCraft standard
 * 2.1: {@code [EntityType] [identifier]: [reason]}, where {@code subject} is
 * the first two parts, e.g. "Team new" or "Retrospective 4".
 */
final class ServiceValidation {

    private ServiceValidation() {
    }

    /** Trims a required text and checks its length. */
    static String requireText(String subject, String field, String value, int maxLength) {
        String trimmed = value == null ? "" : value.strip();
        if (trimmed.isEmpty()) {
            throw new InvalidInputException(subject + ": " + field + " must not be blank");
        }
        if (trimmed.length() > maxLength) {
            throw new InvalidInputException(subject + ": " + field + " must be at most " + maxLength + " characters");
        }
        return trimmed;
    }

    /** Exact, case-sensitive match against the allowed constants (decisions.md F4). */
    @SafeVarargs
    static <E extends Enum<E>> E parseEnum(String subject, String field, String value, E... allowed) {
        return Arrays.stream(allowed)
                .filter(constant -> constant.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new InvalidInputException(
                        subject + ": " + field + " must be one of " + names(allowed)));
    }

    /** Returns the trimmed author if they are a member of the team (decisions.md F1). */
    static String requireMember(Team team, String subject, String author, int maxLength) {
        String name = requireText(subject, "author", author, maxLength);
        if (!team.hasMember(name)) {
            throw new InvalidInputException("Team " + team.getId() + ": '" + name + "' is not a member");
        }
        return name;
    }

    static void requireOpenForNewItem(Retrospective retrospective) {
        if (!retrospective.isOpen()) {
            throw new RetroflowException("Retrospective " + retrospective.getId()
                    + ": cannot add feedback to a closed retrospective");
        }
    }

    /** @param action "modify" or "delete" */
    static void requireOpenForChange(FeedbackItem item, String action) {
        if (!item.getRetrospective().isOpen()) {
            throw new RetroflowException(subject(item) + ": cannot " + action + " an item of a closed retrospective");
        }
    }

    /** "ActionItem 7" or "FeedbackItem 7". */
    static String subject(FeedbackItem item) {
        String type = item instanceof ActionItem ? "ActionItem" : "FeedbackItem";
        return type + " " + item.getId();
    }

    @SafeVarargs
    private static <E extends Enum<E>> String names(E... constants) {
        return Arrays.stream(constants).map(Enum::name).collect(Collectors.joining(", "));
    }

}
