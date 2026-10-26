package com.stackcraft.retroflow.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.MissingNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.stackcraft.retroflow.exception.SummaryUnavailableException;
import com.stackcraft.retroflow.exception.SummaryUnavailableException.Reason;
import com.stackcraft.retroflow.service.SummaryModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.TimeoutException;

/**
 * {@link SummaryModel} on OpenAI's Chat Completions API with structured
 * output: the answer must follow {@link #SCHEMA} ({@code strict: true}).
 * Failures become a {@link SummaryUnavailableException}; the provider's own
 * error text is never passed on (standard 4.2) and the key is never logged
 * (standard 5.1).
 */
public class OpenAiSummaryModel implements SummaryModel {

    /** What the model must return. SummaryValidator checks it again against the database. */
    static final String SCHEMA = """
            {"type": "object", "additionalProperties": false,
             "required": ["wentWell", "needsImprovement", "openActionItems"],
             "properties": {
               "wentWell": {"type": "array", "items": {"type": "string"}},
               "needsImprovement": {"type": "array", "items": {"type": "string"}},
               "openActionItems": {"type": "array", "items": {"type": "object", "additionalProperties": false,
                 "required": ["id", "summary", "priority"],
                 "properties": {"id": {"type": "integer"}, "summary": {"type": "string"},
                                "priority": {"type": "string", "enum": ["LOW", "MEDIUM", "HIGH"]}}}}}}
            """;

    private static final Logger log = LoggerFactory.getLogger(OpenAiSummaryModel.class);
    private static final int TOO_MANY_REQUESTS = 429;

    /** Model settings; {@code apiKey} comes from the OPENAI_API_KEY environment variable. */
    public record Settings(String model, double temperature, String apiKey, String systemPrompt) {

        @Override
        public String toString() {
            return "Settings[model=" + model + ", temperature=" + temperature + ", apiKey=***]";
        }
    }

    private final RestClient client;
    private final ObjectMapper json;
    private final Settings settings;
    private final JsonNode schema;

    public OpenAiSummaryModel(RestClient client, ObjectMapper json, Settings settings) {
        this.client = client;
        this.json = json;
        this.settings = settings;
        this.schema = read(SCHEMA);
    }

    @Override
    public Answer summarise(Input input) {
        if (settings.apiKey() == null || settings.apiKey().isBlank()) {
            log.warn("Summary requested but OPENAI_API_KEY is not set");
            throw new SummaryUnavailableException(input.retrospectiveId(), Reason.NOT_CONFIGURED);
        }
        return answer(input.retrospectiveId(), call(input));
    }

    private JsonNode call(Input input) {
        try {
            return client.post().uri("/chat/completions")
                    .headers(headers -> headers.setBearerAuth(settings.apiKey()))
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request(input))
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientException error) {
            throw failure(input.retrospectiveId(), error);
        }
    }

    /** Timeout, rate limit or any other provider failure; logged without the provider's message. */
    private SummaryUnavailableException failure(Long id, RestClientException error) {
        Reason reason = Reason.PROVIDER_ERROR;
        String detail = causes(error);
        if (error instanceof RestClientResponseException response) {
            int status = response.getStatusCode().value();
            reason = status == TOO_MANY_REQUESTS ? Reason.RATE_LIMITED : Reason.PROVIDER_ERROR;
            detail = "HTTP " + status + " " + errorCode(response);
        } else if (error instanceof ResourceAccessException && isTimeout(error)) {
            reason = Reason.TIMEOUT;
        }
        log.warn("OpenAI summary of retrospective {} failed: {} ({})", id, reason, detail);
        return new SummaryUnavailableException(id, reason);
    }

    private ObjectNode request(Input input) {
        ObjectNode request = json.createObjectNode()
                .put("model", settings.model())
                .put("temperature", settings.temperature());
        request.putArray("messages")
                .add(json.createObjectNode().put("role", "system").put("content", settings.systemPrompt()))
                .add(json.createObjectNode().put("role", "user").put("content", write(input)));
        ObjectNode format = request.putObject("response_format").put("type", "json_schema");
        format.putObject("json_schema").put("name", "retrospective_summary").put("strict", true).set("schema", schema);
        return request;
    }

    /** The JSON text the model produced, as long as it finished and didn't refuse. */
    private Answer answer(Long id, JsonNode response) {
        JsonNode choice = response == null ? MissingNode.getInstance() : response.path("choices").path(0);
        JsonNode message = choice.path("message");
        String finish = choice.path("finish_reason").asText("none");
        if (message.hasNonNull("refusal") || !"stop".equals(finish) || !message.path("content").isTextual()) {
            log.warn("OpenAI gave no usable answer for retrospective {} (finish_reason {}, refused {})",
                    id, finish, message.hasNonNull("refusal"));
            throw new SummaryUnavailableException(id, Reason.INVALID_ANSWER);
        }
        JsonNode usage = response.path("usage");
        return new Answer(message.path("content").asText(), usage.path("prompt_tokens").asLong(),
                usage.path("completion_tokens").asLong());
    }

    /** "ResourceAccessException < HttpTimeoutException": exception types only, never messages. */
    private static String causes(Throwable error) {
        StringBuilder chain = new StringBuilder(error.getClass().getSimpleName());
        for (Throwable cause = error.getCause(); cause != null; cause = cause.getCause()) {
            chain.append(" < ").append(cause.getClass().getSimpleName());
        }
        return chain.toString();
    }

    private static boolean isTimeout(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            // The JDK client's read timeout arrives as an IOException caused by a concurrent TimeoutException.
            if (cause instanceof HttpTimeoutException || cause instanceof SocketTimeoutException
                    || cause instanceof TimeoutException) {
                return true;
            }
        }
        return false;
    }

    /** OpenAI's machine-readable error code (e.g. "rate_limit_exceeded"); never the message. */
    private String errorCode(RestClientResponseException error) {
        try {
            return json.readTree(error.getResponseBodyAsString()).path("error").path("code").asText("unknown");
        } catch (Exception unreadable) {
            return "unknown";
        }
    }

    /** The feedback as JSON data, in a fixed key order, so the same retrospective gives the same prompt. */
    private String write(Input input) {
        try {
            ObjectNode data = json.createObjectNode().put("retrospective", input.title());
            data.set("items", json.valueToTree(input.items()));
            return json.writeValueAsString(data);
        } catch (Exception impossible) {
            throw new IllegalStateException("Summary input could not be written as JSON", impossible);
        }
    }

    private JsonNode read(String text) {
        try {
            return json.readTree(text);
        } catch (Exception impossible) {
            throw new IllegalStateException("Summary schema is not valid JSON", impossible);
        }
    }

}
