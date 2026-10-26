package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.Priority;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.ActionItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.stackcraft.retroflow.service.ServiceValidation.parseEnum;
import static com.stackcraft.retroflow.service.ServiceValidation.requireMember;
import static com.stackcraft.retroflow.service.ServiceValidation.requireOpenForChange;
import static com.stackcraft.retroflow.service.ServiceValidation.requireOpenForNewItem;
import static com.stackcraft.retroflow.service.ServiceValidation.requireText;

/**
 * Action items: adding, editing, completing and filtering. Completion is
 * one-way, and it stays possible after the retrospective is closed because it
 * is follow-up work (decisions.md F6).
 */
@Service
@Transactional(readOnly = true)
public class ActionItemService {

    private static final String NEW_ITEM = "ActionItem new";

    private final ActionItemRepository actionItemRepository;
    private final EntityLookup lookup;

    public ActionItemService(ActionItemRepository actionItemRepository, EntityLookup lookup) {
        this.actionItemRepository = actionItemRepository;
        this.lookup = lookup;
    }

    /**
     * Adds an action item to an OPEN retrospective.
     *
     * @param retrospectiveId id of the retrospective
     * @param content         what needs to be done, trimmed
     * @param priority        "LOW", "MEDIUM" or "HIGH"
     * @param author          name of a member of the retrospective's team
     * @return the saved, not yet completed action item
     * @throws InvalidInputException if the priority or content is invalid, or the author is not a member
     * @throws NotFoundException     if the retrospective does not exist
     * @throws RetroflowException    if the retrospective is closed
     */
    @Transactional
    public ActionItem addActionItem(Long retrospectiveId, String content, String priority, String author) {
        Priority itemPriority = parseEnum(NEW_ITEM, "priority", priority, Priority.values());
        String text = requireText(NEW_ITEM, "content", content, FeedbackItem.MAX_CONTENT_LENGTH);
        Retrospective retrospective = lookup.retrospective(retrospectiveId);
        String member = requireMember(retrospective.getTeam(), NEW_ITEM, author, FeedbackItem.MAX_AUTHOR_LENGTH);
        requireOpenForNewItem(retrospective);
        return actionItemRepository.save(new ActionItem(retrospective, text, itemPriority, member));
    }

    /**
     * Changes the content and priority of an action item.
     *
     * @param actionItemId id of the action item
     * @param content      the new text, trimmed
     * @param priority     "LOW", "MEDIUM" or "HIGH"
     * @return the updated action item
     * @throws NotFoundException     if it does not exist
     * @throws InvalidInputException if the content or priority is invalid
     * @throws RetroflowException    if its retrospective is closed
     */
    @Transactional
    public ActionItem updateActionItem(Long actionItemId, String content, String priority) {
        ActionItem item = lookup.actionItem(actionItemId);
        String subject = "ActionItem " + actionItemId;
        String text = requireText(subject, "content", content, FeedbackItem.MAX_CONTENT_LENGTH);
        Priority itemPriority = parseEnum(subject, "priority", priority, Priority.values());
        requireOpenForChange(item, "modify");
        item.setContent(text);
        item.setPriority(itemPriority);
        return item;
    }

    /**
     * Marks an action item as completed, also when its retrospective is closed.
     *
     * @param actionItemId id of the action item
     * @return the completed action item
     * @throws NotFoundException  if it does not exist
     * @throws RetroflowException if it is already completed
     */
    @Transactional
    public ActionItem completeActionItem(Long actionItemId) {
        ActionItem item = lookup.actionItem(actionItemId);
        if (item.isCompleted()) {
            throw new RetroflowException("ActionItem " + actionItemId + ": already completed");
        }
        item.markCompleted();
        return item;
    }

    /**
     * Completed action items cannot be uncompleted, so this fails loudly for a
     * completed item and changes nothing for an open one (decisions.md A2).
     *
     * @param actionItemId id of the action item
     * @return the unchanged action item, if it was not completed
     * @throws NotFoundException  if it does not exist
     * @throws RetroflowException if it is completed
     */
    @Transactional
    public ActionItem uncompleteActionItem(Long actionItemId) {
        ActionItem item = lookup.actionItem(actionItemId);
        if (item.isCompleted()) {
            throw new RetroflowException("ActionItem " + actionItemId + ": already completed, cannot be undone");
        }
        return item;
    }

    /**
     * Action items across all retrospectives of a team. Both filters are optional.
     *
     * @param teamId    id of the team
     * @param priority  "LOW", "MEDIUM", "HIGH", or null for any
     * @param completed true, false, or null for any
     * @return the matching action items, oldest first
     * @throws NotFoundException     if the team does not exist
     * @throws InvalidInputException if the priority filter is not a known priority
     */
    public List<ActionItem> getActionItemsForTeam(Long teamId, String priority, Boolean completed) {
        lookup.team(teamId);
        Priority priorityFilter = priority == null
                ? null
                : parseEnum("Team " + teamId, "priority filter", priority, Priority.values());
        return actionItemRepository.findForTeam(teamId, priorityFilter, completed);
    }

}
