package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.InvalidInputException;
import com.stackcraft.retroflow.exception.NotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import com.stackcraft.retroflow.repository.TeamRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static com.stackcraft.retroflow.service.ServiceValidation.requireText;

/**
 * Creates and retrieves teams.
 */
@Service
@Transactional(readOnly = true)
public class TeamService {

    private static final String NEW_TEAM = "Team new";

    private final TeamRepository teamRepository;
    private final EntityLookup lookup;

    public TeamService(TeamRepository teamRepository, EntityLookup lookup) {
        this.teamRepository = teamRepository;
        this.lookup = lookup;
    }

    /**
     * Creates a team. Name and member names are trimmed.
     *
     * @param name    team name, unique ignoring case
     * @param members member names, at least one, no duplicates ignoring case
     * @return the saved team
     * @throws InvalidInputException if the name is blank or too long, there are no members,
     *                               a member name is blank or too long, or a member is listed twice
     * @throws RetroflowException    if a team with the same name already exists
     */
    @Transactional
    public Team createTeam(String name, List<String> members) {
        String teamName = requireText(NEW_TEAM, "name", name, Team.MAX_NAME_LENGTH);
        List<String> memberNames = requireMembers(members);
        if (teamRepository.existsByNameIgnoreCase(teamName)) {
            throw new RetroflowException(NEW_TEAM + ": a team named '" + teamName + "' already exists");
        }
        return teamRepository.save(new Team(teamName, memberNames));
    }

    /**
     * @param teamId id of the team
     * @return the team
     * @throws NotFoundException if no team has this id
     */
    public Team getTeam(Long teamId) {
        return lookup.team(teamId);
    }

    /**
     * @return all teams, ordered by name
     */
    public List<Team> getAllTeams() {
        return teamRepository.findAllByOrderByNameAsc();
    }

    private static List<String> requireMembers(List<String> members) {
        if (members == null || members.isEmpty()) {
            throw new InvalidInputException(NEW_TEAM + ": members must not be empty");
        }
        List<String> names = members.stream()
                .map(member -> requireText(NEW_TEAM, "member name", member, Team.MAX_NAME_LENGTH))
                .toList();
        Set<String> seen = new HashSet<>();
        for (String memberName : names) {
            if (!seen.add(memberName.toLowerCase(Locale.ROOT))) {
                throw new InvalidInputException(NEW_TEAM + ": member '" + memberName + "' is listed more than once");
            }
        }
        return names;
    }

}
