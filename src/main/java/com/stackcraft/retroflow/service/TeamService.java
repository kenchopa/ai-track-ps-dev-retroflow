package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Creates and retrieves teams.
 *
 * <p>STUB (Block 1): signatures and contracts only. The bodies are
 * implemented in Block 2; see decisions.md and test-matrix.md.
 */
@Service
@Transactional(readOnly = true)
public class TeamService {

    private final TeamRepository teamRepository;

    public TeamService(TeamRepository teamRepository) {
        this.teamRepository = teamRepository;
    }

    /**
     * Creates a team. Name and member names are trimmed.
     *
     * @param name    team name, unique ignoring case
     * @param members member names, at least one, no duplicates ignoring case
     * @return the saved team
     * @throws InvalidInputException if there are no members or a member name is duplicated
     * @throws RetroflowException    if a team with the same name already exists
     */
    @Transactional
    public Team createTeam(String name, List<String> members) {
        throw new UnsupportedOperationException("TeamService.createTeam is not implemented yet");
    }

    /**
     * @param teamId id of the team
     * @return the team
     * @throws NotFoundException if no team has this id
     */
    public Team getTeam(Long teamId) {
        throw new UnsupportedOperationException("TeamService.getTeam is not implemented yet");
    }

    /**
     * @return all teams, ordered by name
     */
    public List<Team> getAllTeams() {
        throw new UnsupportedOperationException("TeamService.getAllTeams is not implemented yet");
    }

}
