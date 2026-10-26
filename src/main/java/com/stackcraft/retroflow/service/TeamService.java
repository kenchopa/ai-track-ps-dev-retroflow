package com.stackcraft.retroflow.service;

import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.exception.ResourceNotFoundException;
import com.stackcraft.retroflow.exception.RetroflowException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TeamService {

    /**
     * Creates a team with the supplied member names.
     *
     * @param name the team name
     * @param members the nonempty list of member names
     * @return the saved team with its members initialized
     * @throws RetroflowException if a team business rule is violated
     * @throws UnsupportedOperationException until the service is implemented
     */
    public Team createTeam(String name, List<String> members) {
        throw new UnsupportedOperationException("Team new: creation is not implemented");
    }

    /**
     * Retrieves a team and initializes its member names for response mapping.
     *
     * @param id the team identifier
     * @return the team with its members initialized
     * @throws ResourceNotFoundException if the team does not exist
     * @throws UnsupportedOperationException until the service is implemented
     */
    public Team getTeam(Long id) {
        throw new UnsupportedOperationException("Team " + id + ": retrieval is not implemented");
    }
}
