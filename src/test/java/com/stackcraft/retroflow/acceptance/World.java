package com.stackcraft.retroflow.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * What one scenario knows: the ids behind the names used in the feature file, and the last
 * HTTP response. Team names get a per-scenario suffix, because all tests share one H2 database
 * and team names must be unique (decision T1).
 */
public class World {

    /** An id that never exists, for the "not found" scenarios. */
    static final long UNKNOWN_ID = 999_999_999L;

    private static final ObjectMapper JSON = new ObjectMapper();
    /** {Sprint 42} or {name:Payments squad}; never a JSON object, because aliases contain no quotes or braces. */
    private static final Pattern PLACEHOLDER = Pattern.compile("\\{(name:)?([^{}\"]+)}");

    private final TestRestTemplate http;
    private final String suffix = " #" + UUID.randomUUID().toString().substring(0, 6);
    private final Map<String, Long> ids = new HashMap<>();
    private final Map<String, String> retroOfItem = new HashMap<>();
    private Response last;

    World(TestRestTemplate http) {
        this.http = http;
    }

    /** The status, the parsed JSON body (or null) and the headers of one HTTP call. */
    public record Response(int status, JsonNode body, HttpHeaders headers) {

        String error() {
            return body == null ? "" : body.path("error").asText();
        }
    }

    Response send(HttpMethod method, String path, Object body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        Object payload = body instanceof String text ? text : write(body);
        ResponseEntity<String> response = http.exchange(path, method, new HttpEntity<>(payload, headers), String.class);
        last = new Response(response.getStatusCode().value(), read(response.getBody()), response.getHeaders());
        return last;
    }

    /** Like {@link #send}, for setup steps: anything but 2xx fails the scenario right there. */
    Response require(HttpMethod method, String path, Object body) {
        Response response = send(method, path, body);
        assertThat(response.status()).as("%s %s answered %s", method, path, response.body()).isBetween(200, 299);
        return response;
    }

    Response last() {
        assertThat(last).as("no request was sent yet").isNotNull();
        return last;
    }

    /** "Payments squad" becomes "Payments squad #a1b2c3": unique per scenario, same case as written. */
    String teamName(String alias) {
        return alias + suffix;
    }

    void remember(String alias, long id) {
        ids.put(alias, id);
    }

    /** An item, and the retrospective it belongs to (to look it up again later). */
    void rememberItem(String alias, long id, String retroAlias) {
        remember(alias, id);
        retroOfItem.put(alias, retroAlias);
    }

    String retroOf(String itemAlias) {
        String retro = retroOfItem.get(itemAlias);
        assertThat(retro).as("no item '%s' was added in this scenario", itemAlias).isNotNull();
        return retro;
    }

    long id(String alias) {
        Long id = ids.get(alias);
        assertThat(id).as("nothing called '%s' was created in this scenario", alias).isNotNull();
        return id;
    }

    /**
     * Fills in placeholders in an expected text: {Sprint 42} becomes its id, {name:Payments squad}
     * the team's real name, {unknown} an id that doesn't exist.
     */
    String resolve(String text) {
        Matcher matcher = PLACEHOLDER.matcher(text);
        StringBuilder out = new StringBuilder();
        while (matcher.find()) {
            String alias = matcher.group(2);
            String value;
            if (matcher.group(1) != null) {
                value = teamName(alias);
            } else if (alias.equals("unknown")) {
                value = String.valueOf(UNKNOWN_ID);
            } else {
                value = String.valueOf(id(alias));
            }
            matcher.appendReplacement(out, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(out);
        return out.toString();
    }

    private static String write(Object body) {
        try {
            return body == null ? null : JSON.writeValueAsString(body);
        } catch (Exception impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    private static JsonNode read(String body) {
        try {
            return body == null || body.isBlank() ? null : JSON.readTree(body);
        } catch (Exception notJson) {
            return null;
        }
    }
}
