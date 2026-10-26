package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.repository.ActionItemRepository;
import com.stackcraft.retroflow.repository.FeedbackItemRepository;
import com.stackcraft.retroflow.repository.RetrospectiveRepository;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.springframework.stereotype.Component;

/**
 * Finds entities by id or throws {@link NotFoundException}. Shared by the
 * services so every "not found" has the same message.
 */
@Component
public class EntityLookup {

    private final TeamRepository teamRepository;
    private final RetrospectiveRepository retrospectiveRepository;
    private final FeedbackItemRepository feedbackItemRepository;
    private final ActionItemRepository actionItemRepository;

    public EntityLookup(TeamRepository teamRepository,
                        RetrospectiveRepository retrospectiveRepository,
                        FeedbackItemRepository feedbackItemRepository,
                        ActionItemRepository actionItemRepository) {
        this.teamRepository = teamRepository;
        this.retrospectiveRepository = retrospectiveRepository;
        this.feedbackItemRepository = feedbackItemRepository;
        this.actionItemRepository = actionItemRepository;
    }

    /**
     * @param id team id
     * @return the team
     * @throws NotFoundException if it does not exist
     */
    public Team team(Long id) {
        return teamRepository.findById(id).orElseThrow(() -> new NotFoundException("Team", id));
    }

    /**
     * Like {@link #team(Long)}, but locks the team row until the transaction ends.
     *
     * @param id team id
     * @return the locked team
     * @throws NotFoundException if it does not exist
     */
    public Team teamForUpdate(Long id) {
        return teamRepository.findByIdForUpdate(id).orElseThrow(() -> new NotFoundException("Team", id));
    }

    /**
     * @param id retrospective id
     * @return the retrospective
     * @throws NotFoundException if it does not exist
     */
    public Retrospective retrospective(Long id) {
        return retrospectiveRepository.findById(id).orElseThrow(() -> new NotFoundException("Retrospective", id));
    }

    /**
     * @param id feedback item id; action items are feedback items too
     * @return the item
     * @throws NotFoundException if it does not exist
     */
    public FeedbackItem feedbackItem(Long id) {
        return feedbackItemRepository.findById(id).orElseThrow(() -> new NotFoundException("FeedbackItem", id));
    }

    /**
     * @param id action item id
     * @return the action item
     * @throws NotFoundException if it does not exist or is not an action item
     */
    public ActionItem actionItem(Long id) {
        return actionItemRepository.findById(id).orElseThrow(() -> new NotFoundException("ActionItem", id));
    }

}
