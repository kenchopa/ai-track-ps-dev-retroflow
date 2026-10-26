package com.stackcraft.retroflow.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * A retrospective held by a team on a given date. It starts OPEN and can be
 * closed exactly once; there is deliberately no way to reopen it, so status
 * has no setter (see decisions.md R5/E5).
 */
@Entity
@Table(name = "retrospective")
public class Retrospective {

    public static final int MAX_TITLE_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "team_id", nullable = false)
    private Team team;

    @NotBlank
    @Size(max = MAX_TITLE_LENGTH)
    @Column(name = "title", nullable = false, length = MAX_TITLE_LENGTH)
    private String title;

    @NotNull
    @Column(name = "retro_date", nullable = false)
    private LocalDate date;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private RetrospectiveStatus status = RetrospectiveStatus.OPEN;

    protected Retrospective() {
        // required by JPA
    }

    public Retrospective(Team team, String title, LocalDate date) {
        this.team = team;
        this.title = title;
        this.date = date;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Team getTeam() {
        return team;
    }

    public void setTeam(Team team) {
        this.team = team;
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

    public boolean isOpen() {
        return status == RetrospectiveStatus.OPEN;
    }

    /**
     * Moves the retrospective to CLOSED. Whether closing is allowed (e.g. not
     * already closed) is a business rule checked by the service layer.
     */
    public void close() {
        this.status = RetrospectiveStatus.CLOSED;
    }

}
