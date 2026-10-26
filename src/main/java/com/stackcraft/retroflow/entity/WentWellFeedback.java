package com.stackcraft.retroflow.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("WENT_WELL")
public class WentWellFeedback extends FeedbackItem {

    public WentWellFeedback() {
    }

    public WentWellFeedback(Retrospective retrospective, String content, String submittedBy) {
        super(retrospective, content, submittedBy);
    }

    public WentWellFeedback(Long id, long version, Retrospective retrospective, String content, String submittedBy) {
        super(id, version, retrospective, content, submittedBy);
    }

    @Override
    public FeedbackType getType() {
        return FeedbackType.WENT_WELL;
    }
}
