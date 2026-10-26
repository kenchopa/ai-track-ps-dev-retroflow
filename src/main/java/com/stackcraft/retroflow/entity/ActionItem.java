package com.stackcraft.retroflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;

/**
 * A feedback item of type ACTION_ITEM that also tracks a priority and whether
 * it has been completed.
 *
 * <p>Stored in the {@code feedback_item} table (SINGLE_TABLE inheritance), so
 * {@code priority} and {@code completed} are nullable columns; their
 * presence is guaranteed by validation and by this class, not by the schema.
 *
 * <p>Completion is one-way: there is {@link #markCompleted()} but no setter,
 * so an item cannot be uncompleted by accident (decisions.md E5). Whether a
 * transition is allowed is checked by the service layer.
 */
@Entity
@DiscriminatorValue("ACTION")
public class ActionItem extends FeedbackItem {

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 10)
    private Priority priority;

    @Column(name = "completed")
    private boolean completed;

    protected ActionItem() {
        // required by JPA
    }

    public ActionItem(Retrospective retrospective, String content, Priority priority, String author) {
        super(retrospective, content, author);
        this.priority = priority;
    }

    public Priority getPriority() {
        return priority;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void markCompleted() {
        this.completed = true;
    }

}
