package com.stackcraft.retroflow.controller;

import com.stackcraft.retroflow.config.ApiJsonConfiguration;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.entity.RetrospectiveStatus;
import com.stackcraft.retroflow.entity.Team;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.TeamService;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

@WebMvcTest({TeamController.class, RetrospectiveController.class})
@Import(ApiJsonConfiguration.class)
@MockBean({TeamService.class, RetrospectiveService.class})
abstract class ControllerTestSupport {

    protected static final Long TEAM_ID = 7L;
    protected static final Long RETRO_ID = 42L;
    protected static final LocalDate MEETING_DATE = LocalDate.of(2026, 10, 26);
    protected static final String TEAM_JSON = """
        {"name":"Alpha","members":["Alice"]}
        """;
    protected static final String RETRO_JSON = """
        {"title":"Sprint retro","date":"2026-10-26"}
        """;
    protected final MockMvc mvc;
    protected final TeamService teams;
    protected final RetrospectiveService retrospectives;

    ControllerTestSupport(MockMvc mvc, TeamService teams, RetrospectiveService retrospectives) {
        this.mvc = mvc;
        this.teams = teams;
        this.retrospectives = retrospectives;
    }

    protected Team team() {
        return new Team(TEAM_ID, 0, "Alpha", List.of("Alice"), List.of());
    }

    protected Retrospective retrospective() {
        return new Retrospective(RETRO_ID, 0, team(), "Sprint retro", MEETING_DATE,
            RetrospectiveStatus.OPEN, List.of());
    }
}
