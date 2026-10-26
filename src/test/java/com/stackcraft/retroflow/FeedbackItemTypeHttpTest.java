package com.stackcraft.retroflow;

import org.junit.jupiter.api.DisplayName;
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
 * audit-findings.md C1 over HTTP on the running app (QA 1.1): posting ACTION_ITEM
 * to the feedback endpoint (test-matrix row 28, decisions.md F5) is a 400 with the
 * service's §2.1 message, so the entity guard is never reached and never surfaces
 * as a 500. Each test creates its own team with a unique name (QA 3.1, 3.2).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FeedbackItemTypeHttpTest {

    private static final String MEMBER = "Alice";
    private static final String TITLE = "Sprint 1";
    private static final String CONTENT = "Pairing worked well";
    private static final String ACTION_ITEM = "ACTION_ITEM";
    private static final String EXPECTED_ERROR = "FeedbackItem new: type ACTION_ITEM must be added as an action item";

    private final TestRestTemplate http;

    @Autowired
    FeedbackItemTypeHttpTest(TestRestTemplate http) {
        this.http = http;
    }

    @Test
    @DisplayName("Row 28 / C1: ACTION_ITEM via the feedback endpoint is a 400 with a §2.1 message, nothing saved")
    void addFeedbackItem_withActionItemType_returns400WithMessageAndSavesNothing() {
        // Arrange
        long retroId = createOpenRetro();
        String itemsUrl = "/api/retrospectives/" + retroId + "/feedback-items";
        Map<String, String> body = Map.of("content", CONTENT, "type", ACTION_ITEM, "author", MEMBER);

        // Act
        ResponseEntity<Map> response = http.postForEntity(itemsUrl, body, Map.class);

        // Assert: status AND body (QA 2.2), exact message (coding 2.1), and no item stored
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().get("error")).isEqualTo(EXPECTED_ERROR);
        assertThat(response.getBody().get("status")).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(http.getForEntity(itemsUrl, List.class).getBody()).isEmpty();
    }

    private long createOpenRetro() {
        String uniqueName = "Team " + UUID.randomUUID();
        long teamId = idOf(http.postForEntity("/api/teams",
                Map.of("name", uniqueName, "members", List.of(MEMBER)), Map.class));
        return idOf(http.postForEntity("/api/teams/" + teamId + "/retrospectives",
                Map.of("title", TITLE), Map.class));
    }

    private static long idOf(ResponseEntity<Map> created) {
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return ((Number) created.getBody().get("id")).longValue();
    }

}
