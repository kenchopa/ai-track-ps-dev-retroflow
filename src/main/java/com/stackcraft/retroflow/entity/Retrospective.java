package com.stackcraft.retroflow.entity;

import com.stackcraft.retroflow.exception.RetroflowException;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(
    name = "retrospectives",
    indexes = @Index(name = "idx_retrospectives_team_status", columnList = "team_id,status")
)
public class Retrospective {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long version;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false, updatable = false)
    private Team team;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String title;

    @NotNull
    @Column(name = "meeting_date", nullable = false)
    private LocalDate date;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private RetrospectiveStatus status = RetrospectiveStatus.OPEN;

    @OneToMany(mappedBy = "retrospective", fetch = FetchType.LAZY)
    private List<FeedbackItem> feedbackItems = new ArrayList<>();

    public Retrospective() {
    }

    public Retrospective(Team team, String title, LocalDate date) {
        setTeam(team);
        setTitle(title);
        setDate(date);
    }

    public Retrospective(Long id, long version, Team team, String title, LocalDate date,
                         RetrospectiveStatus status, List<FeedbackItem> feedbackItems) {
        setId(id);
        setVersion(version);
        setTitle(title);
        setDate(date);
        setFeedbackItems(feedbackItems);
        setStatus(status);
        setTeam(team);
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

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        Objects.requireNonNull(team, "team must not be null");
        if (this.team != null && this.team != team) {
            throw new RetroflowException("Retrospective " + id + ": team cannot be changed");
        }
        this.team = team;
        team.addRetrospective(this);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public RetrospectiveStatus getStatus() {
        return status;
    }

    public void setStatus(RetrospectiveStatus status) {
        Objects.requireNonNull(status, "status must not be null");
        if (this.status == RetrospectiveStatus.CLOSED && status != RetrospectiveStatus.CLOSED) {
            throw new RetroflowException("Retrospective " + id + ": cannot reopen a closed retrospective");
        }
        this.status = status;
    }

    public List<FeedbackItem> getFeedbackItems() {
        return Collections.unmodifiableList(feedbackItems);
    }

    public void setFeedbackItems(List<FeedbackItem> feedbackItems) {
        var replacement = new ArrayList<>(Objects.requireNonNull(feedbackItems, "feedbackItems must not be null"));
        if (!replacement.containsAll(this.feedbackItems)
                || replacement.stream().anyMatch(item -> item == null || item.getRetrospective() != this)
                || replacement.stream().distinct().count() != replacement.size()) {
            throw new RetroflowException("Retrospective " + id + ": feedback ownership cannot be changed");
        }
        this.feedbackItems.clear();
        this.feedbackItems.addAll(replacement);
    }

    public void close() {
        setStatus(RetrospectiveStatus.CLOSED);
    }

    void addFeedbackItem(FeedbackItem item) {
        if (status == RetrospectiveStatus.CLOSED) {
            throw new RetroflowException("Retrospective " + id + ": cannot add feedback to a closed retrospective");
        }
        if (!feedbackItems.contains(item)) {
            feedbackItems.add(item);
        }
    }
}
