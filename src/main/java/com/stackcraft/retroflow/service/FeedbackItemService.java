package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.FeedbackType;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.FeedbackItemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.stackcraft.retroflow.service.ServiceValidation.parseEnum;
import static com.stackcraft.retroflow.service.ServiceValidation.requireMember;
import static com.stackcraft.retroflow.service.ServiceValidation.requireOpenForChange;
import static com.stackcraft.retroflow.service.ServiceValidation.requireOpenForNewItem;
import static com.stackcraft.retroflow.service.ServiceValidation.requireText;
import static com.stackcraft.retroflow.service.ServiceValidation.subject;

/**
 * Feedback items of a retrospective. The operations that change items
 * (add, modify, delete) only work while the retrospective is OPEN.
 * Modify and delete also apply to action items, which are feedback items too.
 */
@Service
@Transactional(readOnly = true)
public class FeedbackItemService {

    private static final String NEW_ITEM = "FeedbackItem new";

    private final FeedbackItemRepository feedbackItemRepository;
    private final EntityLookup lookup;

    public FeedbackItemService(FeedbackItemRepository feedbackItemRepository, EntityLookup lookup) {
        this.feedbackItemRepository = feedbackItemRepository;
        this.lookup = lookup;
    }

    /**
     * Adds a WENT_WELL or NEEDS_IMPROVEMENT item to an OPEN retrospective.
     *
     * @param retrospectiveId id of the retrospective
     * @param content         the feedback text, trimmed
     * @param type            "WENT_WELL" or "NEEDS_IMPROVEMENT"; action items have their own operation
     * @param author          name of a member of the retrospective's team
     * @return the saved item
     * @throws InvalidInputException if the type or content is invalid, or the author is not a member
     * @throws NotFoundException     if the retrospective does not exist
     * @throws RetroflowException    if the retrospective is closed
     */
    @Transactional
    public FeedbackItem addFeedbackItem(Long retrospectiveId, String content, String type, String author) {
        FeedbackType feedbackType = parseType(type);
        String text = requireText(NEW_ITEM, "content", content, FeedbackItem.MAX_CONTENT_LENGTH);
        Retrospective retrospective = lookup.retrospective(retrospectiveId);
        String member = requireMember(retrospective.getTeam(), NEW_ITEM, author, FeedbackItem.MAX_AUTHOR_LENGTH);
        requireOpenForNewItem(retrospective);
        return feedbackItemRepository.save(new FeedbackItem(retrospective, text, feedbackType, member));
    }

    /**
     * @param retrospectiveId id of the retrospective
     * @return all its items, action items included, in the order they were added
     * @throws NotFoundException if the retrospective does not exist
     */
    public List<FeedbackItem> getFeedbackItems(Long retrospectiveId) {
        lookup.retrospective(retrospectiveId);
        return feedbackItemRepository.findByRetrospectiveIdOrderByIdAsc(retrospectiveId);
    }

    /**
     * Changes the content of a feedback item (or action item). Type and author never change.
     *
     * @param feedbackItemId id of the item
     * @param content        the new text, trimmed
     * @return the updated item
     * @throws NotFoundException     if the item does not exist
     * @throws InvalidInputException if the content is blank or too long
     * @throws RetroflowException    if the item's retrospective is closed
     */
    @Transactional
    public FeedbackItem updateFeedbackItem(Long feedbackItemId, String content) {
        FeedbackItem item = lookup.feedbackItem(feedbackItemId);
        String text = requireText(subject(item), "content", content, FeedbackItem.MAX_CONTENT_LENGTH);
        requireOpenForChange(item, "modify");
        item.setContent(text);
        return item;
    }

    /**
     * Deletes a feedback item (or action item).
     *
     * @param feedbackItemId id of the item
     * @throws NotFoundException  if the item does not exist
     * @throws RetroflowException if the item's retrospective is closed
     */
    @Transactional
    public void deleteFeedbackItem(Long feedbackItemId) {
        FeedbackItem item = lookup.feedbackItem(feedbackItemId);
        requireOpenForChange(item, "delete");
        feedbackItemRepository.delete(item);
    }

    private static FeedbackType parseType(String type) {
        if (FeedbackType.ACTION_ITEM.name().equals(type)) {
            throw new InvalidInputException(NEW_ITEM + ": type ACTION_ITEM must be added as an action item");
        }
        return parseEnum(NEW_ITEM, "type", type, FeedbackType.WENT_WELL, FeedbackType.NEEDS_IMPROVEMENT);
    }

}
