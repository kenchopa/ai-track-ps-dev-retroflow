package com.stackcraft.retroflow.entity;

import com.stackcraft.retroflow.exception.RetroflowException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Check;

import java.util.Objects;

@Entity
@Table(
    name = "feedback_items",
    indexes = {
        @Index(name = "idx_feedback_retrospective", columnList = "retrospective_id"),
        @Index(name = "idx_feedback_type_priority_completed", columnList = "feedback_type,priority,completed")
    }
)
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "feedback_type", discriminatorType = DiscriminatorType.STRING, length = 20)
@Check(constraints = """
    (feedback_type = 'ACTION_ITEM' AND priority IS NOT NULL AND completed IS NOT NULL)
    OR (feedback_type IN ('WENT_WELL', 'NEEDS_IMPROVEMENT') AND priority IS NULL AND completed IS NULL)
    """)
public abstract class FeedbackItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long version;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retrospective_id", nullable = false, updatable = false)
    private Retrospective retrospective;

    @NotBlank
    @Size(max = 10_000)
    @Column(nullable = false, length = 10_000)
    private String content;

    @NotBlank
    @Size(max = 200)
    @Column(name = "submitted_by", nullable = false, length = 200)
    private String submittedBy;

    public FeedbackItem() {
    }

    protected FeedbackItem(Retrospective retrospective, String content, String submittedBy) {
        setRetrospective(retrospective);
        setContent(content);
        setSubmittedBy(submittedBy);
    }

    public FeedbackItem(Long id, long version, Retrospective retrospective, String content, String submittedBy) {
        this(retrospective, content, submittedBy);
        setId(id);
        setVersion(version);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }

    public Retrospective getRetrospective() {
        return retrospective;
    }

    public void setRetrospective(Retrospective retrospective) {
        Objects.requireNonNull(retrospective, "retrospective must not be null");
        if (this.retrospective != null && this.retrospective != retrospective) {
            throw new RetroflowException("FeedbackItem " + id + ": retrospective cannot be changed");
        }
        if (this.retrospective != retrospective) {
            retrospective.addFeedbackItem(this);
            this.retrospective = retrospective;
        }
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        requireEditable();
        this.content = content;
    }

    public String getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(String submittedBy) {
        requireEditable();
        this.submittedBy = submittedBy;
    }

    protected void requireEditable() {
        if (retrospective != null && retrospective.getStatus() == RetrospectiveStatus.CLOSED) {
            throw new RetroflowException("FeedbackItem " + id + ": cannot modify feedback in a closed retrospective");
        }
    }

    @Transient
    public abstract FeedbackType getType();
}
