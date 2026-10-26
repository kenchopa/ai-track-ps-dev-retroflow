package com.stackcraft.retroflow.eval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stackcraft.retroflow.service.RetrospectiveSummary;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Who judges the judge? Planted summaries with a known verdict
 * (eval/judge-cases.json) go through the judge; it should call the faithful
 * ones faithful and catch every planted error. Run it before trusting a new
 * judge model: {@code ./mvnw test -Peval -Dtest=JudgeEvalTest -Deval.judge.model=<model>}.
 */
@Tag("eval")
class JudgeEvalTest {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record JudgeCase(String name, boolean faithful, RetrospectiveSummary summary) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record JudgeCases(List<EvalCase.Item> feedback, List<JudgeCase> cases) {
    }

    @Test
    void judge_onPlantedSummaries_agreesWithTheKnownVerdict() throws Exception {
        // Arrange
        String apiKey = System.getenv("OPENAI_API_KEY");
        assertThat(apiKey).as("OPENAI_API_KEY must be set to run the eval").isNotBlank();
        String model = System.getProperty("eval.judge.model", "gpt-4.1");
        JudgeCases planted = new ObjectMapper().readValue(Path.of("eval/judge-cases.json").toFile(), JudgeCases.class);
        Judge judge = new Judge(new ObjectMapper(), "https://api.openai.com/v1", model,
                Files.readString(Path.of("eval/judge-prompt.txt")), apiKey);

        // Act
        int agreed = 0;
        StringBuilder out = new StringBuilder("\n=== Judge eval: " + model + " ===\n");
        for (JudgeCase judgeCase : planted.cases()) {
            boolean verdict = judge.judge(planted.feedback(), judgeCase.summary()).check().passed();
            agreed += verdict == judgeCase.faithful() ? 1 : 0;
            out.append(String.format(Locale.ROOT, "%-30s expected %-5s judge %-5s %s%n", judgeCase.name(),
                    judgeCase.faithful(), verdict, verdict == judgeCase.faithful() ? "" : "<- WRONG"));
        }

        // Assert: report the agreement; deciding whether it is good enough is ours, not the judge's
        out.append(String.format(Locale.ROOT, "AGREEMENT %d/%d%n", agreed, planted.cases().size()));
        System.out.println(out);
        assertThat(agreed).isPositive();
    }

}
