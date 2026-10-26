package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.Team;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TeamRepository extends JpaRepository<Team, Long> {

    boolean existsByNameIgnoreCase(String name);

    List<Team> findAllByOrderByNameAsc();

    /**
     * Loads a team and locks its row until the transaction ends, so two
     * concurrent requests cannot both open a retrospective (decisions.md R8).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM Team t WHERE t.id = :id")
    Optional<Team> findByIdForUpdate(@Param("id") Long id);
}
