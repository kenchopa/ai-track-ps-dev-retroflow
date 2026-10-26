package com.stackcraft.retroflow.repository;

import com.stackcraft.retroflow.entity.FeedbackItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Feedback items of all kinds; action items are included (SINGLE_TABLE inheritance).
 */
public interface FeedbackItemRepository extends JpaRepository<FeedbackItem, Long> {

    List<FeedbackItem> findByRetrospectiveIdOrderByIdAsc(Long retrospectiveId);
}
