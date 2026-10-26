package com.stackcraft.retroflow.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stackcraft.retroflow.service.SummaryModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Wires the OpenAI summary model. Settings live in application.properties;
 * the API key only comes from the OPENAI_API_KEY environment variable
 * (standard 5.1). Without a key the app still starts, and a summary
 * request answers 503 instead of calling anything.
 */
@Configuration
public class OpenAiConfig {

    @Bean
    SummaryModel summaryModel(RestClient.Builder builder, ObjectMapper json,
                              @Value("${retroflow.summary.base-url}") String baseUrl,
                              @Value("${retroflow.summary.timeout}") Duration timeout,
                              @Value("${retroflow.summary.model}") String model,
                              @Value("${retroflow.summary.temperature}") double temperature,
                              @Value("${OPENAI_API_KEY:}") String apiKey,
                              @Value("${retroflow.summary.prompt}") Resource prompt) throws IOException {
        var settings = new OpenAiSummaryModel.Settings(model, temperature, apiKey,
                prompt.getContentAsString(StandardCharsets.UTF_8));
        return new OpenAiSummaryModel(restClient(builder, baseUrl, timeout), json, settings);
    }

    /** The HTTP client with connect and read timeouts; shared with the timeout test. */
    static RestClient restClient(RestClient.Builder builder, String baseUrl, Duration timeout) {
        JdkClientHttpRequestFactory requests = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder().connectTimeout(timeout).build());
        requests.setReadTimeout(timeout);
        return builder.baseUrl(baseUrl).requestFactory(requests).build();
    }

}
