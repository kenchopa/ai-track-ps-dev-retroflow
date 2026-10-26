package com.stackcraft.retroflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * A piece of feedback submitted by a team member during a retrospective.
 *
 * <p>{@link ActionItem} extends this class using SINGLE_TABLE inheritance, so
 * all items of a retrospective are read with one query (decisions.md E1).
 * The {@code item_kind} discriminator says which Java class a row is; the
 * {@code type} column is the business type from the brief. A plain
 * FeedbackItem is never of type ACTION_ITEM (decisions.md E2).
 *
 * <p>Type and author are immutable; only content can change, and only while
 * the retrospective is OPEN (enforced in the service layer).
 */
@Entity
@Table(name = "feedback_item")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "item_kind", length = 20)
@DiscriminatorValue("FEEDBACK")
public class FeedbackItem {

    public static final int MAX_CONTENT_LENGTH = 2000;
    public static final int MAX_AUTHOR_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "retrospective_id", nullable = false)
    private Retrospective retrospective;

    @NotBlank
    @Size(max = MAX_CONTENT_LENGTH)
    @Column(name = "content", nullable = false, length = MAX_CONTENT_LENGTH)
    private String content;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, updatable = false, length = 20)
    private FeedbackType type;

    @NotBlank
    @Size(max = MAX_AUTHOR_LENGTH)
    @Column(name = "author", nullable = false, updatable = false, length = MAX_AUTHOR_LENGTH)
    private String author;

    protected FeedbackItem() {
        // required by JPA
    }

    /**
     * Creates a WENT_WELL or NEEDS_IMPROVEMENT item.
     *
     * @throws IllegalArgumentException if type is ACTION_ITEM; use {@link ActionItem} instead
     */
    public FeedbackItem(Retrospective retrospective, String content, FeedbackType type, String author) {
        if (type == FeedbackType.ACTION_ITEM) {
            throw new IllegalArgumentException("Use ActionItem for feedback of type ACTION_ITEM");
        }
        this.retrospective = retrospective;
        this.content = content;
        this.type = type;
        this.author = author;
    }

    /**
     * Used by {@link ActionItem}: creates an item of type ACTION_ITEM.
     */
    protected FeedbackItem(Retrospective retrospective, String content, String author) {
        this.retrospective = retrospective;
        this.content = content;
        this.type = FeedbackType.ACTION_ITEM;
        this.author = author;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Retrospective getRetrospective() {
        return retrospective;
    }

    public void setRetrospective(Retrospective retrospective) {
        this.retrospective = retrospective;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public FeedbackType getType() {
        return type;
    }

    public String getAuthor() {
        return author;
    }

}
