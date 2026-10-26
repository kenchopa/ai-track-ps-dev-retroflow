package com.stackcraft.retroflow.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(nullable = false)
    private long version;

    @NotBlank
    @Size(max = 200)
    @Column(nullable = false, length = 200)
    private String name;

    @NotEmpty
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
        name = "team_members",
        joinColumns = @JoinColumn(name = "team_id", nullable = false)
    )
    @OrderColumn(name = "member_position")
    @Column(name = "member_name", nullable = false, length = 200)
    private List<@NotBlank @Size(max = 200) String> members = new ArrayList<>();

    @OneToMany(mappedBy = "team", fetch = FetchType.LAZY)
    private List<Retrospective> retrospectives = new ArrayList<>();

    public Team() {
    }

    public Team(String name, List<String> members) {
        setName(name);
        setMembers(members);
    }

    public Team(Long id, long version, String name, List<String> members, List<Retrospective> retrospectives) {
        this(name, members);
        setId(id);
        setVersion(version);
        setRetrospectives(retrospectives);
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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getMembers() {
        return Collections.unmodifiableList(members);
    }

    public void setMembers(List<String> members) {
        var replacement = new ArrayList<>(Objects.requireNonNull(members, "members must not be null"));
        this.members.clear();
        this.members.addAll(replacement);
    }

    public List<Retrospective> getRetrospectives() {
        return Collections.unmodifiableList(retrospectives);
    }

    public void setRetrospectives(List<Retrospective> retrospectives) {
        var replacement = new ArrayList<>(Objects.requireNonNull(retrospectives, "retrospectives must not be null"));
        if (!replacement.containsAll(this.retrospectives)
                || replacement.stream().anyMatch(retro -> retro == null || retro.getTeam() != this)
                || replacement.stream().distinct().count() != replacement.size()) {
            throw new IllegalArgumentException("Team " + id + ": retrospective ownership cannot be changed");
        }
        this.retrospectives.clear();
        this.retrospectives.addAll(replacement);
    }

    void addRetrospective(Retrospective retrospective) {
        if (!retrospectives.contains(retrospective)) {
            retrospectives.add(retrospective);
        }
    }
}
