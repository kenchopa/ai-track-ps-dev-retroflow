package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.exception.SummaryUnavailableException;
import com.stackcraft.retroflow.repository.FeedbackItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Summarises a closed retrospective with a language model. The rule ("only
 * a CLOSED retrospective") lives here like every other rule; the model's
 * answer is validated by {@link SummaryValidator} before anyone sees it.
 * Not transactional on purpose: the model call can take seconds, and it
 * must not hold a database connection while it waits.
 */
@Service
public class SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);

    private final EntityLookup lookup;
    private final FeedbackItemRepository feedbackItemRepository;
    private final SummaryModel model;
    private final int maxItems;

    public SummaryService(EntityLookup lookup, FeedbackItemRepository feedbackItemRepository, SummaryModel model,
                          @Value("${retroflow.summary.max-items}") int maxItems) {
        this.lookup = lookup;
        this.feedbackItemRepository = feedbackItemRepository;
        this.model = model;
        this.maxItems = maxItems;
    }

    /**
     * Summarises a CLOSED retrospective: what went well, what needs improvement and the open action items.
     *
     * @param retrospectiveId id of the retrospective
     * @return the validated summary
     * @throws NotFoundException            if the retrospective does not exist
     * @throws RetroflowException           if it is still OPEN
     * @throws InvalidInputException        if it has more feedback items than a summary can take
     * @throws SummaryUnavailableException  if the model fails, or its answer does not hold up
     */
    public RetrospectiveSummary summarise(Long retrospectiveId) {
        Retrospective retrospective = lookup.retrospective(retrospectiveId);
        if (retrospective.isOpen()) {
            throw new RetroflowException("Retrospective " + retrospectiveId
                    + ": only a closed retrospective can be summarised");
        }
        List<FeedbackItem> items = feedbackItemRepository.findByRetrospectiveIdOrderByIdAsc(retrospectiveId);
        if (items.size() > maxItems) {
            throw new InvalidInputException("Retrospective " + retrospectiveId + ": has " + items.size()
                    + " feedback items, a summary takes at most " + maxItems);
        }
        SummaryModel.Answer answer = model.summarise(new SummaryModel.Input(
                retrospectiveId, retrospective.getTitle(), items.stream().map(SummaryService::toModelItem).toList()));
        log.info("Summary of retrospective {}: {} items, {} input tokens, {} output tokens",
                retrospectiveId, items.size(), answer.inputTokens(), answer.outputTokens());
        return SummaryValidator.validate(retrospectiveId, answer.json(), items);
    }

    private static SummaryModel.Item toModelItem(FeedbackItem item) {
        if (item instanceof ActionItem action) {
            return new SummaryModel.Item(action.getId(), action.getType().name(), action.getContent(),
                    action.getPriority().name(), action.isCompleted());
        }
        return new SummaryModel.Item(item.getId(), item.getType().name(), item.getContent(), null, null);
    }

}
