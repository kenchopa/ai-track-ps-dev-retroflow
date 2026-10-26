package com.stackcraft.retroflow.entity;

import com.stackcraft.retroflow.exception.RetroflowException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

@Entity
@DiscriminatorValue("ACTION_ITEM")
public class ActionItem extends FeedbackItem {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(length = 6)
    private Priority priority;

    @Column
    private boolean completed = false;

    public ActionItem() {
    }

    public ActionItem(Retrospective retrospective, String content, Priority priority, String submittedBy) {
        super(retrospective, content, submittedBy);
        setPriority(priority);
    }

    public ActionItem(Long id, long version, Retrospective retrospective, String content, String submittedBy,
                      Priority priority, boolean completed) {
        super(id, version, retrospective, content, submittedBy);
        setPriority(priority);
        setCompleted(completed);
    }

    @Override
    public FeedbackType getType() {
        return FeedbackType.ACTION_ITEM;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        requireEditable();
        this.priority = priority;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        if (this.completed && !completed) {
            throw new RetroflowException("ActionItem " + getId() + ": already completed, cannot be undone");
        }
        this.completed = completed;
    }

    public void complete() {
        setCompleted(true);
    }
}
