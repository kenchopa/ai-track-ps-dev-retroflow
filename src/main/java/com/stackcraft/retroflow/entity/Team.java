package com.stackcraft.retroflow.entity;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.List;

/**
 * A team of people who run retrospectives together. Members are stored as
 * plain names (no authentication). A team must have at least one member.
 *
 * <p>Retrospectives reference their team; the team does not hold a
 * collection of retrospectives (see decisions.md E3).
 */
@Entity
@Table(name = "team")
public class Team {

    public static final int MAX_NAME_LENGTH = 100;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = MAX_NAME_LENGTH)
    @Column(name = "name", nullable = false, unique = true, length = MAX_NAME_LENGTH)
    private String name;

    @NotEmpty
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "team_member", joinColumns = @JoinColumn(name = "team_id"))
    @OrderColumn(name = "position")
    @Column(name = "member_name", nullable = false, length = MAX_NAME_LENGTH)
    private List<@NotBlank @Size(max = MAX_NAME_LENGTH) String> members = new ArrayList<>();

    protected Team() {
        // required by JPA
    }

    public Team(String name, List<String> members) {
        this.name = name;
        this.members = new ArrayList<>(members);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getMembers() {
        return List.copyOf(members);
    }

    public void setMembers(List<String> members) {
        this.members = new ArrayList<>(members);
    }

    /**
     * @param memberName name to look up
     * @return true if the name matches a member, ignoring case and surrounding whitespace
     */
    public boolean hasMember(String memberName) {
        if (memberName == null) {
            return false;
        }
        String wanted = memberName.strip();
        return members.stream().anyMatch(member -> member.equalsIgnoreCase(wanted));
    }

}
