package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.RetrospectiveRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static com.stackcraft.retroflow.service.ServiceValidation.requireText;

/**
 * Retrospective lifecycle. Also offers the feedback and action item
 * operations the vendor tests ({@code RetroflowBusinessRulesTest}) call on
 * this service; those delegate to {@link FeedbackItemService} and
 * {@link ActionItemService}, where the rules live.
 */
@Service
@Transactional(readOnly = true)
public class RetrospectiveService {

    private final RetrospectiveRepository retrospectiveRepository;
    private final EntityLookup lookup;
    private final FeedbackItemService feedbackItemService;
    private final ActionItemService actionItemService;

    public RetrospectiveService(RetrospectiveRepository retrospectiveRepository,
                                EntityLookup lookup,
                                FeedbackItemService feedbackItemService,
                                ActionItemService actionItemService) {
        this.retrospectiveRepository = retrospectiveRepository;
        this.lookup = lookup;
        this.feedbackItemService = feedbackItemService;
        this.actionItemService = actionItemService;
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
     * Creates an OPEN retrospective for a team. The team row is locked while
     * checking, so two concurrent requests cannot both succeed.
     *
     * @param teamId id of the team
     * @param title  title of the retrospective, trimmed
     * @param date   date of the retrospective, or null for today
     * @return the saved retrospective
     * @throws InvalidInputException if the title is blank or too long
     * @throws NotFoundException     if the team does not exist
     * @throws RetroflowException    if the team already has an OPEN retrospective
     */
    @Transactional
    public Retrospective createRetrospective(Long teamId, String title, LocalDate date) {
        String cleanTitle = requireText("Retrospective new", "title", title, Retrospective.MAX_TITLE_LENGTH);
        Team team = lookup.teamForUpdate(teamId);
        retrospectiveRepository.findFirstByTeamIdAndStatus(teamId, RetrospectiveStatus.OPEN)
                .ifPresent(open -> {
                    throw new RetroflowException("Team " + teamId + ": already has an open retrospective ("
                            + open.getId() + ")");
                });
        LocalDate retroDate = date != null ? date : LocalDate.now();
        return retrospectiveRepository.save(new Retrospective(team, cleanTitle, retroDate));
    }

    /**
     * @param retrospectiveId id of the retrospective
     * @return the retrospective
     * @throws NotFoundException if it does not exist
     */
    public Retrospective getRetrospective(Long retrospectiveId) {
        return lookup.retrospective(retrospectiveId);
    }

    /**
     * @param teamId id of the team
     * @return the team's retrospectives, newest date first
     * @throws NotFoundException if the team does not exist
     */
    public List<Retrospective> getRetrospectivesForTeam(Long teamId) {
        lookup.team(teamId);
        return retrospectiveRepository.findByTeamIdOrderByDateDescIdDesc(teamId);
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
        Retrospective retrospective = lookup.retrospective(retrospectiveId);
        if (!retrospective.isOpen()) {
            throw new RetroflowException("Retrospective " + retrospectiveId + ": already closed");
        }
        retrospective.close();
        return retrospective;
    }

    /**
     * Delegates to {@link FeedbackItemService#addFeedbackItem(Long, String, String, String)}.
     *
     * @param retrospectiveId id of the retrospective
     * @param content         the feedback text
     * @param type            "WENT_WELL" or "NEEDS_IMPROVEMENT"
     * @param author          name of a member of the retrospective's team
     * @return the saved item
     * @throws NotFoundException     if the retrospective does not exist
     * @throws InvalidInputException if the input is invalid or the author is not a member
     * @throws RetroflowException    if the retrospective is closed
     */
    @Transactional
    public FeedbackItem addFeedbackItem(Long retrospectiveId, String content, String type, String author) {
        return feedbackItemService.addFeedbackItem(retrospectiveId, content, type, author);
    }

    /**
     * Delegates to {@link ActionItemService#addActionItem(Long, String, String, String)}.
     *
     * @param retrospectiveId id of the retrospective
     * @param content         what needs to be done
     * @param priority        "LOW", "MEDIUM" or "HIGH"
     * @param author          name of a member of the retrospective's team
     * @return the saved, not yet completed action item
     * @throws NotFoundException     if the retrospective does not exist
     * @throws InvalidInputException if the input is invalid or the author is not a member
     * @throws RetroflowException    if the retrospective is closed
     */
    @Transactional
    public ActionItem addActionItem(Long retrospectiveId, String content, String priority, String author) {
        return actionItemService.addActionItem(retrospectiveId, content, priority, author);
    }

    /**
     * Delegates to {@link ActionItemService#completeActionItem(Long)}.
     *
     * @param actionItemId id of the action item
     * @return the completed action item
     * @throws NotFoundException  if it does not exist
     * @throws RetroflowException if it is already completed
     */
    @Transactional
    public ActionItem completeActionItem(Long actionItemId) {
        return actionItemService.completeActionItem(actionItemId);
    }

    /**
     * Delegates to {@link ActionItemService#uncompleteActionItem(Long)}.
     *
     * @param actionItemId id of the action item
     * @return the unchanged action item, if it was not completed
     * @throws NotFoundException  if it does not exist
     * @throws RetroflowException if it is completed
     */
    @Transactional
    public ActionItem uncompleteActionItem(Long actionItemId) {
        return actionItemService.uncompleteActionItem(actionItemId);
    }

}
