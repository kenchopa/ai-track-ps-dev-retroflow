package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.FeedbackItem;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class RetrospectiveService {

    /**
     * Creates an open retrospective for a team on an explicit meeting date.
     *
     * @param teamId the team identifier
     * @param title the retrospective title
     * @param date the meeting date
     * @return the saved retrospective
     * @throws ResourceNotFoundException if the team does not exist
     * @throws RetroflowException if the team already has an open retrospective
     * @throws UnsupportedOperationException until the service is implemented
     */
    public Retrospective createRetrospective(Long teamId, String title, LocalDate date) {
        throw new UnsupportedOperationException("Team " + teamId + ": retrospective creation is not implemented");
    }

    /**
     * Preserves the inherited test contract without deciding a default date.
     *
     * @param teamId the team identifier
     * @param title the retrospective title
     * @return the saved retrospective
     * @throws ResourceNotFoundException if the team does not exist
     * @throws RetroflowException if the team already has an open retrospective
     * @throws UnsupportedOperationException until the service and date default are implemented
     */
    public Retrospective createRetrospective(Long teamId, String title) {
        throw new UnsupportedOperationException("Team " + teamId + ": retrospective creation is not implemented");
    }

    /**
     * Lists a team's retrospectives, distinguishing a missing team from an empty history.
     *
     * @param teamId the team identifier
     * @return the retrospectives, or an empty list for an existing team without history
     * @throws ResourceNotFoundException if the team does not exist
     * @throws UnsupportedOperationException until the service is implemented
     */
    public List<Retrospective> listRetrospectives(Long teamId) {
        throw new UnsupportedOperationException("Team " + teamId + ": retrospective listing is not implemented");
    }

    /**
     * Closes a retrospective without allowing it to be reopened.
     *
     * @param id the retrospective identifier
     * @throws ResourceNotFoundException if the retrospective does not exist
     * @throws RetroflowException if the requested transition violates the agreed closure policy
     * @throws UnsupportedOperationException until the service is implemented
     */
    public void closeRetrospective(Long id) {
        throw new UnsupportedOperationException("Retrospective " + id + ": closure is not implemented");
    }

    /**
     * Preserves the inherited feedback test contract; no HTTP operation is exposed here.
     *
     * @param retrospectiveId the retrospective identifier
     * @param content the feedback text
     * @param type the feedback type
     * @param submittedBy the submitting member name
     * @return the saved feedback
     * @throws ResourceNotFoundException if the retrospective does not exist
     * @throws RetroflowException if feedback cannot be added
     * @throws UnsupportedOperationException until the service is implemented
     */
    public FeedbackItem addFeedbackItem(Long retrospectiveId, String content, String type, String submittedBy) {
        throw new UnsupportedOperationException("Retrospective " + retrospectiveId + ": feedback is not implemented");
    }

    /**
     * Preserves the inherited action creation test contract.
     *
     * @param retrospectiveId the retrospective identifier
     * @param content the action text
     * @param priority the action priority
     * @param submittedBy the submitting member name
     * @return the saved action
     * @throws ResourceNotFoundException if the retrospective does not exist
     * @throws RetroflowException if the action cannot be added
     * @throws UnsupportedOperationException until the service is implemented
     */
    public ActionItem addActionItem(Long retrospectiveId, String content, String priority, String submittedBy) {
        throw new UnsupportedOperationException("Retrospective " + retrospectiveId + ": actions are not implemented");
    }

    /**
     * Preserves the inherited action completion test contract.
     *
     * @param id the action identifier
     * @throws ResourceNotFoundException if the action does not exist
     * @throws RetroflowException if completion violates the agreed policy
     * @throws UnsupportedOperationException until the service is implemented
     */
    public void completeActionItem(Long id) {
        throw new UnsupportedOperationException("ActionItem " + id + ": completion is not implemented");
    }

    /**
     * Preserves the inherited test contract for rejecting reversal of completion.
     *
     * @param id the action identifier
     * @throws ResourceNotFoundException if the action does not exist
     * @throws RetroflowException if the action is already completed
     * @throws UnsupportedOperationException until the service is implemented
     */
    public void uncompleteActionItem(Long id) {
        throw new UnsupportedOperationException("ActionItem " + id + ": uncompletion is not implemented");
    }
}
