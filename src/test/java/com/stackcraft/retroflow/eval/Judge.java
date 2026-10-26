package com.stackcraft.retroflow.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stackcraft.retroflow.service.RetrospectiveSummary;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;

/**
 * The one check code can't do: is every point backed by the feedback?
 * A model judges that, with the rubric in {@code eval/judge-prompt.txt}, at
 * temperature 0, and its reason is kept in the results so a human can
 * disagree with it. Default judge: gpt-4.1, a larger model than the one it
 * judges; {@link JudgeEvalTest} checks it on planted cases.
 */
final class Judge {

    private static final String SCHEMA = """
            {"type": "object", "additionalProperties": false, "required": ["faithful", "unsupported", "reason"],
             "properties": {"faithful": {"type": "boolean"}, "unsupported": {"type": "array", "items": {"type": "string"}},
                            "reason": {"type": "string"}}}""";
    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    record Verdict(EvalChecks.Check check, long inputTokens, long outputTokens) {
    }

    private final RestClient client;
    private final ObjectMapper json;
    private final String model;
    private final String rubric;
    private final String apiKey;

    Judge(ObjectMapper json, String baseUrl, String model, String rubric, String apiKey) {
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(HttpClient.newHttpClient());
        requests.setReadTimeout(TIMEOUT);
        this.client = RestClient.builder().baseUrl(baseUrl).requestFactory(requests).build();
        this.json = json;
        this.model = model;
        this.rubric = rubric;
        this.apiKey = apiKey;
    }

    Verdict judge(List<EvalCase.Item> feedback, RetrospectiveSummary summary) throws Exception {
        JsonNode response = client.post().uri("/chat/completions")
                .headers(headers -> headers.setBearerAuth(apiKey))
                .contentType(MediaType.APPLICATION_JSON)
                .body(request(feedback, summary))
                .retrieve()
                .body(JsonNode.class);
        JsonNode verdict = json.readTree(response.path("choices").path(0).path("message").path("content").asText());
        boolean faithful = verdict.path("faithful").asBoolean(false);
        String detail = faithful ? "" : verdict.path("unsupported") + " (" + verdict.path("reason").asText() + ")";
        JsonNode usage = response.path("usage");
        return new Verdict(new EvalChecks.Check("faithful to the feedback (judge)", faithful, detail),
                usage.path("prompt_tokens").asLong(), usage.path("completion_tokens").asLong());
    }

    private ObjectNode request(List<EvalCase.Item> feedback, RetrospectiveSummary summary) throws Exception {
        ObjectNode data = json.createObjectNode();
        data.set("feedback", json.valueToTree(feedback));
        data.set("summary", json.valueToTree(summary));
        ObjectNode request = json.createObjectNode().put("model", model).put("temperature", 0);
        request.putArray("messages")
                .add(json.createObjectNode().put("role", "system").put("content", rubric))
                .add(json.createObjectNode().put("role", "user").put("content", json.writeValueAsString(data)));
        request.putObject("response_format").put("type", "json_schema").putObject("json_schema")
                .put("name", "faithfulness_verdict").put("strict", true).set("schema", json.readTree(SCHEMA));
        return request;
    }

}
