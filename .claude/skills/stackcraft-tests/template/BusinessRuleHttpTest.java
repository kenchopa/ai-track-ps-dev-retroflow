package com.stackcraft.retroflow;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Template for the stackcraft-tests skill: one business rule, tested over HTTP
 * against the running application (StackCraft QA 1.1). Copy the layout, not the
 * rule. Each test creates its own team with a unique name (QA 3.1, 3.2).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BusinessRuleHttpTest {

    private static final String MEMBER = "Alice";
    private static final String FIRST_TITLE = "Sprint 1";
    private static final String SECOND_TITLE = "Sprint 2";

    @Autowired
    private TestRestTemplate http;

    @Test
    void createRetrospective_whenTeamAlreadyHasOpenRetro_returns409WithMessage() {
        // Arrange
        long teamId = createTeam();
        post("/api/teams/" + teamId + "/retrospectives", Map.of("title", FIRST_TITLE));

        // Act
        ResponseEntity<Map> response = post("/api/teams/" + teamId + "/retrospectives", Map.of("title", SECOND_TITLE));

        // Assert: status AND body (QA 2.2), exact message format (coding 2.1)
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat((String) response.getBody().get("error"))
                .startsWith("Team " + teamId + ": already has an open retrospective");
    }

    private long createTeam() {
        String uniqueName = "Team " + UUID.randomUUID();
        ResponseEntity<Map> created = post("/api/teams", Map.of("name", uniqueName, "members", List.of(MEMBER)));
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) created.getBody().get("id")).longValue();
    }

    private ResponseEntity<Map> post(String path, Object body) {
        return http.postForEntity(path, body, Map.class);
    }

}
