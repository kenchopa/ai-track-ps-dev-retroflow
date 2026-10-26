package com.stackcraft.retroflow.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.stackcraft.retroflow.exception.SummaryUnavailableException;
import com.stackcraft.retroflow.exception.SummaryUnavailableException.Reason;
import com.stackcraft.retroflow.service.SummaryModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;



import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * The OpenAI client against a fake HTTP server: the request it sends, and
 * what every kind of failure turns into. Never calls the real API.
 */
class OpenAiSummaryModelTest {

    private static final String BASE_URL = "https://openai.test/v1";
    private static final String FAKE_KEY = "test-key-not-real";
    private static final String MODEL = "gpt-test";
    private static final Duration SHORT_TIMEOUT = Duration.ofMillis(200);
    private static final Duration SLOW_ANSWER = Duration.ofSeconds(2);
    private static final Long RETRO_ID = 12L;
    private static final String ANSWER = "{\"wentWell\": [], \"needsImprovement\": [], \"openActionItems\": []}";
    private static final SummaryModel.Input INPUT = new SummaryModel.Input(RETRO_ID, "Sprint 1",
            List.of(new SummaryModel.Item(1L, "WENT_WELL", "Pairing worked well", null, null)));

    private final RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
    private final MockRestServiceServer provider = MockRestServiceServer.bindTo(builder).build();

    private OpenAiSummaryModel model(String apiKey) {
        var settings = new OpenAiSummaryModel.Settings(MODEL, 0.2, apiKey, "You summarise retrospectives.");
        return new OpenAiSummaryModel(builder.build(), new ObjectMapper(), settings);
    }

    private static String completion(String finishReason, String refusal) {
        return """
                {"choices": [{"finish_reason": "%s", "message": {"role": "assistant", "content": %s, "refusal": %s}}],
                 "usage": {"prompt_tokens": 321, "completion_tokens": 45}}"""
                .formatted(finishReason, new ObjectMapper().valueToTree(ANSWER), refusal == null ? "null" : "\"" + refusal + "\"");
    }

    @Test
    void summarise_whenProviderAnswers_sendsStructuredOutputRequestAndReturnsJsonWithTokens() {
        // Arrange
        provider.expect(requestTo(BASE_URL + "/chat/completions"))
                .andExpect(method(POST))
                .andExpect(header("Authorization", "Bearer " + FAKE_KEY))
                .andExpect(jsonPath("$.model").value(MODEL))
                .andExpect(jsonPath("$.response_format.type").value("json_schema"))
                .andExpect(jsonPath("$.response_format.json_schema.strict").value(true))
                .andExpect(jsonPath("$.response_format.json_schema.schema.required[1]").value("needsImprovement"))
                .andExpect(jsonPath("$.messages[0].role").value("system"))
                .andExpect(jsonPath("$.messages[1].content").value(containsString("Pairing worked well")))
                .andExpect(content().string(not(containsString("author"))))
                .andRespond(withSuccess(completion("stop", null), MediaType.APPLICATION_JSON));

        // Act
        SummaryModel.Answer answer = model(FAKE_KEY).summarise(INPUT);

        // Assert
        assertThat(answer.json()).isEqualTo(ANSWER);
        assertThat(answer.inputTokens()).isEqualTo(321);
        assertThat(answer.outputTokens()).isEqualTo(45);
        provider.verify();
    }

    @ParameterizedTest(name = "HTTP {0} becomes {1}")
    @CsvSource({"429, RATE_LIMITED", "500, PROVIDER_ERROR", "503, PROVIDER_ERROR", "401, PROVIDER_ERROR", "400, PROVIDER_ERROR"})
    void summarise_whenProviderAnswersWithError_throwsReasonWithoutProviderDetails(int status, Reason expected) {
        // Arrange: a provider error body that echoes part of a key, as real providers do
        String body = "{\"error\": {\"message\": \"Incorrect API key provided: sk-proj-****abcd\", \"code\": \"x\"}}";
        provider.expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withStatus(HttpStatus.valueOf(status)).contentType(MediaType.APPLICATION_JSON).body(body));

        // Act & Assert
        assertThatThrownBy(() -> model(FAKE_KEY).summarise(INPUT))
                .isExactlyInstanceOf(SummaryUnavailableException.class)
                .hasMessageStartingWith("Retrospective " + RETRO_ID + ": ")
                .hasMessageNotContaining("sk-")
                .hasMessageNotContaining("Incorrect")
                .extracting("reason").isEqualTo(expected);
    }

    @Test
    void summarise_whenProviderTimesOut_throwsTimeout() {
        // Arrange
        provider.expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        // Act & Assert
        assertThatThrownBy(() -> model(FAKE_KEY).summarise(INPUT))
                .isExactlyInstanceOf(SummaryUnavailableException.class)
                .hasMessage("Retrospective " + RETRO_ID + ": the summary took too long, try again later")
                .extracting("reason").isEqualTo(Reason.TIMEOUT);
    }

    @Test
    void summarise_whenRealHttpClientTimesOut_throwsTimeout() throws Exception {
        // Arrange: a local server that answers after 2 seconds, and the production HTTP client with a 200 ms timeout
        HttpServer slow = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        slow.createContext("/v1/chat/completions", exchange -> {
            sleepQuietly(SLOW_ANSWER);
            exchange.sendResponseHeaders(200, -1);
            exchange.close();
        });
        slow.start();
        String baseUrl = "http://localhost:" + slow.getAddress().getPort() + "/v1";
        var settings = new OpenAiSummaryModel.Settings(MODEL, 0.2, FAKE_KEY, "prompt");
        var model = new OpenAiSummaryModel(OpenAiConfig.restClient(RestClient.builder(), baseUrl, SHORT_TIMEOUT),
                new ObjectMapper(), settings);

        // Act & Assert
        try {
            assertThatThrownBy(() -> model.summarise(INPUT))
                    .isExactlyInstanceOf(SummaryUnavailableException.class)
                    .extracting("reason").isEqualTo(Reason.TIMEOUT);
        } finally {
            slow.stop(0);
        }
    }

    private static void sleepQuietly(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    @ParameterizedTest(name = "finish_reason {0}, refusal {1}")
    @CsvSource(value = {"length, null", "content_filter, null", "stop, I can't help with that"}, nullValues = "null")
    void summarise_whenModelRefusesOrIsCutOff_throwsInvalidAnswer(String finishReason, String refusal) {
        // Arrange
        provider.expect(requestTo(BASE_URL + "/chat/completions"))
                .andRespond(withSuccess(completion(finishReason, refusal), MediaType.APPLICATION_JSON));

        // Act & Assert
        assertThatThrownBy(() -> model(FAKE_KEY).summarise(INPUT))
                .isExactlyInstanceOf(SummaryUnavailableException.class)
                .extracting("reason").isEqualTo(Reason.INVALID_ANSWER);
    }

    @Test
    void summarise_withoutApiKey_throwsNotConfiguredAndCallsNothing() {
        // Arrange
        String noKey = "";

        // Act & Assert
        assertThatThrownBy(() -> model(noKey).summarise(INPUT))
                .isExactlyInstanceOf(SummaryUnavailableException.class)
                .hasMessage("Retrospective " + RETRO_ID + ": summaries are not available, the model is not configured");
        provider.verify();
    }

    @Test
    void settingsToString_always_hidesTheKey() {
        // Arrange
        var settings = new OpenAiSummaryModel.Settings(MODEL, 0.2, FAKE_KEY, "prompt");

        // Act
        String printed = settings.toString();

        // Assert
        assertThat(printed).doesNotContain(FAKE_KEY).contains("apiKey=***");
    }

}
