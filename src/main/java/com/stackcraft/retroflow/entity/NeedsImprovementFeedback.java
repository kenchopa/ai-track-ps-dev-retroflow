package com.stackcraft.retroflow.entity;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

@Entity
@DiscriminatorValue("NEEDS_IMPROVEMENT")
public class NeedsImprovementFeedback extends FeedbackItem {

    public NeedsImprovementFeedback() {
    }

    public NeedsImprovementFeedback(Retrospective retrospective, String content, String submittedBy) {
        super(retrospective, content, submittedBy);
    }

    public NeedsImprovementFeedback(Long id, long version, Retrospective retrospective,
                                    String content, String submittedBy) {
        super(id, version, retrospective, content, submittedBy);
    }

    @Override
    public FeedbackType getType() {
        return FeedbackType.NEEDS_IMPROVEMENT;
    }
}
