package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.ActionItem;
import com.stackcraft.retroflow.entity.Priority;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ActionItemRepository extends JpaRepository<ActionItem, Long> {

    /**
     * Action items across all retrospectives of a team. A null filter matches everything.
     */
    @Query("""
            SELECT a FROM ActionItem a
            WHERE a.retrospective.team.id = :teamId
              AND (:priority IS NULL OR a.priority = :priority)
              AND (:completed IS NULL OR a.completed = :completed)
            ORDER BY a.id
            """)
    List<ActionItem> findForTeam(@Param("teamId") Long teamId,
                                 @Param("priority") Priority priority,
                                 @Param("completed") Boolean completed);
}
