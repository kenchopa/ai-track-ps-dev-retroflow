package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RetrospectiveRepository extends JpaRepository<Retrospective, Long> {

    Optional<Retrospective> findFirstByTeamIdAndStatus(Long teamId, RetrospectiveStatus status);

    List<Retrospective> findByTeamIdOrderByDateDescIdDesc(Long teamId);
}
