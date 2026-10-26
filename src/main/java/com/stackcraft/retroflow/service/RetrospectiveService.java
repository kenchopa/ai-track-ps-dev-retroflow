package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.FeedbackItemRepository;
import com.stackcraft.retroflow.repository.RetrospectiveRepository;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Retrospective lifecycle, plus the feedback and action item operations the
 * vendor tests ({@code RetroflowBusinessRulesTest}) call on this service.
 *
 * <p>STUB (Block 1): signatures and contracts only. The bodies are
 * implemented in Block 2; see decisions.md and test-matrix.md. Business
 * rules belong here, not in the controllers.
 */
@Service
@Transactional(readOnly = true)
public class RetrospectiveService {

    private final TeamRepository teamRepository;
    private final RetrospectiveRepository retrospectiveRepository;
    private final FeedbackItemRepository feedbackItemRepository;

    public RetrospectiveService(TeamRepository teamRepository,
                                RetrospectiveRepository retrospectiveRepository,
                                FeedbackItemRepository feedbackItemRepository) {
        this.teamRepository = teamRepository;
        this.retrospectiveRepository = retrospectiveRepository;
        this.feedbackItemRepository = feedbackItemRepository;
    }

    /**
     * Creates an OPEN retrospective dated today.
     *
     * @see #createRetrospective(Long, String, LocalDate)
     */
    @Transactional
    public Retrospective createRetrospective(Long teamId, String title) {
        return createRetrospective(teamId, title, null);
    }

    /**
     * Creates an OPEN retrospective for a team.
     *
     * @param teamId id of the team
     * @param title  title of the retrospective
     * @param date   date of the retrospective, or null for today
     * @return the saved retrospective
     * @throws NotFoundException  if the team does not exist
     * @throws RetroflowException if the team already has an OPEN retrospective
     */
    @Transactional
    public Retrospective createRetrospective(Long teamId, String title, LocalDate date) {
        throw new UnsupportedOperationException("RetrospectiveService.createRetrospective is not implemented yet");
    }

    /**
     * @param retrospectiveId id of the retrospective
     * @return the retrospective
     * @throws NotFoundException if it does not exist
     */
    public Retrospective getRetrospective(Long retrospectiveId) {
        throw new UnsupportedOperationException("RetrospectiveService.getRetrospective is not implemented yet");
    }

    /**
     * @param teamId id of the team
     * @return the team's retrospectives, newest date first
     * @throws NotFoundException if the team does not exist
     */
    public List<Retrospective> getRetrospectivesForTeam(Long teamId) {
        throw new UnsupportedOperationException("RetrospectiveService.getRetrospectivesForTeam is not implemented yet");
    }

    /**
     * Closes a retrospective. There is no way back: closed retrospectives cannot be reopened.
     *
     * @param retrospectiveId id of the retrospective
     * @return the closed retrospective
     * @throws NotFoundException  if it does not exist
     * @throws RetroflowException if it is already closed
     */
    @Transactional
    public Retrospective closeRetrospective(Long retrospectiveId) {
        throw new UnsupportedOperationException("RetrospectiveService.closeRetrospective is not implemented yet");
    }

    /**
     * Adds a WENT_WELL or NEEDS_IMPROVEMENT item to an OPEN retrospective.
     *
     * @param retrospectiveId id of the retrospective
     * @param content         the feedback text
     * @param type            "WENT_WELL" or "NEEDS_IMPROVEMENT"
     * @param author          name of a member of the retrospective's team
     * @return the saved item
     * @throws NotFoundException     if the retrospective does not exist
     * @throws InvalidInputException if the type is unknown or ACTION_ITEM, or the author is not a member
     * @throws RetroflowException    if the retrospective is closed
     */
    @Transactional
    public FeedbackItem addFeedbackItem(Long retrospectiveId, String content, String type, String author) {
        throw new UnsupportedOperationException("RetrospectiveService.addFeedbackItem is not implemented yet");
    }

    /**
     * Adds an action item to an OPEN retrospective.
     *
     * @param retrospectiveId id of the retrospective
     * @param content         what needs to be done
     * @param priority        "LOW", "MEDIUM" or "HIGH"
     * @param author          name of a member of the retrospective's team
     * @return the saved, not yet completed action item
     * @throws NotFoundException     if the retrospective does not exist
     * @throws InvalidInputException if the priority is unknown or the author is not a member
     * @throws RetroflowException    if the retrospective is closed
     */
    @Transactional
    public ActionItem addActionItem(Long retrospectiveId, String content, String priority, String author) {
        throw new UnsupportedOperationException("RetrospectiveService.addActionItem is not implemented yet");
    }

    /**
     * Marks an action item as completed. Allowed even when its retrospective is closed (decisions.md F6).
     *
     * @param actionItemId id of the action item
     * @return the completed action item
     * @throws NotFoundException  if it does not exist
     * @throws RetroflowException if it is already completed
     */
    @Transactional
    public ActionItem completeActionItem(Long actionItemId) {
        throw new UnsupportedOperationException("RetrospectiveService.completeActionItem is not implemented yet");
    }

    /**
     * Completed action items cannot be uncompleted. This method exists so the
     * rule is explicit: it always fails for a completed item.
     *
     * @param actionItemId id of the action item
     * @return the unchanged action item, if it was not completed
     * @throws NotFoundException  if it does not exist
     * @throws RetroflowException if it is completed
     */
    @Transactional
    public ActionItem uncompleteActionItem(Long actionItemId) {
        throw new UnsupportedOperationException("RetrospectiveService.uncompleteActionItem is not implemented yet");
    }

}
