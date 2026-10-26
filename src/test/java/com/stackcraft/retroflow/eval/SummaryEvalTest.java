package com.stackcraft.retroflow.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.stackcraft.retroflow.entity.Retrospective;
import com.stackcraft.retroflow.exception.SummaryUnavailableException;
import com.stackcraft.retroflow.service.FeedbackItemService;
import com.stackcraft.retroflow.service.RetrospectiveService;
import com.stackcraft.retroflow.service.RetrospectiveSummary;
import com.stackcraft.retroflow.service.SummaryModel;
import com.stackcraft.retroflow.service.TeamService;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Evaluation of the retrospective summary against the real model: every case
 * in eval/summary-cases.json goes through the real service, validator and
 * OpenAI client. Costs money, so it is tagged "eval" and only runs with
 * {@code ./mvnw test -Peval} (see eval/README.md). Never in the normal build or CI.
 */
@Tag("eval")
@SpringBootTest
@Import(SummaryEvalTest.CountingConfig.class)
class SummaryEvalTest {

    private static final String MEMBER = "Alice";

    private final TeamService teamService;
    private final RetrospectiveService retrospectiveService;
    private final FeedbackItemService feedbackItemService;
    private final CountingSummaryModel tokens;
    private final Map<String, Object> settings;
    private final String judgeModel;
    private final String apiKey;

    @Autowired
    SummaryEvalTest(TeamService teamService, RetrospectiveService retrospectiveService, FeedbackItemService feedbackItemService,
                CountingSummaryModel tokens, @Value("${retroflow.summary.model}") String model,
                @Value("${retroflow.summary.temperature}") double temperature,
                @Value("${eval.judge.model:gpt-4.1-mini}") String judgeModel, @Value("${OPENAI_API_KEY:}") String apiKey) {
        this.teamService = teamService;
        this.retrospectiveService = retrospectiveService;
        this.feedbackItemService = feedbackItemService;
        this.tokens = tokens;
        this.settings = Map.of("model", model, "temperature", temperature, "judge", judgeModel);
        this.judgeModel = judgeModel;
        this.apiKey = apiKey;
    }

    @Test
    void summaryEval_onEveryCase_printsScoreAndCost() throws Exception {
        // Arrange
        assertThat(apiKey).as("OPENAI_API_KEY must be set to run the eval").isNotBlank();
        String label = System.getProperty("eval.label", "run");
        List<EvalCase> cases = EvalCase.load(Path.of("eval/summary-cases.json"));
        Judge judge = new Judge(new ObjectMapper(), "https://api.openai.com/v1", judgeModel,
                Files.readString(Path.of("eval/judge-prompt.txt")), apiKey);
        long start = System.currentTimeMillis();

        // Act
        List<EvalReport.CaseResult> results = new ArrayList<>();
        long[] judgeTokens = new long[2];
        for (EvalCase evalCase : cases) {
            results.add(run(evalCase, judge, judgeTokens));
        }

        // Assert: print and keep the numbers; the score is the result, not a pass/fail
        var candidate = new EvalReport.Usage((String) settings.get("model"), tokens.inputTokens(), tokens.outputTokens());
        var judged = new EvalReport.Usage(judgeModel, judgeTokens[0], judgeTokens[1]);
        System.out.println(EvalReport.print(label + " " + settings, results, candidate, judged, System.currentTimeMillis() - start));
        EvalReport.write(Path.of("eval/results", label + ".json"), label, settings, results, candidate, judged);
        assertThat(results).hasSameSizeAs(cases);
    }

    private EvalReport.CaseResult run(EvalCase evalCase, Judge judge, long[] judgeTokens) throws Exception {
        Retrospective retro = closedRetroWith(evalCase.items());
        try {
            RetrospectiveSummary summary = feedbackItemService.summarise(retro.getId());
            List<EvalChecks.Check> checks = new ArrayList<>(EvalChecks.check(evalCase, summary));
            Judge.Verdict verdict = judge.judge(evalCase.items(), summary);
            judgeTokens[0] += verdict.inputTokens();
            judgeTokens[1] += verdict.outputTokens();
            checks.add(verdict.check());
            return new EvalReport.CaseResult(evalCase.name(), evalCase.tricky(), checks, summary);
        } catch (SummaryUnavailableException failed) {
            List<EvalChecks.Check> checks = new ArrayList<>(EvalChecks.failed(evalCase, failed.getReason().name()));
            checks.add(new EvalChecks.Check("faithful to the feedback (judge)", false, "no summary"));
            return new EvalReport.CaseResult(evalCase.name(), evalCase.tricky(), checks, null);
        }
    }

    private Retrospective closedRetroWith(List<EvalCase.Item> items) {
        Long teamId = teamService.createTeam("Eval team " + UUID.randomUUID(), List.of(MEMBER)).getId();
        Retrospective retro = retrospectiveService.createRetrospective(teamId, "Sprint under evaluation");
        for (EvalCase.Item item : items) {
            if ("ACTION_ITEM".equals(item.type())) {
                Long id = retrospectiveService.addActionItem(retro.getId(), item.content(), item.priority(), MEMBER).getId();
                if (item.completed()) {
                    retrospectiveService.completeActionItem(id);
                }
            } else {
                retrospectiveService.addFeedbackItem(retro.getId(), item.content(), item.type(), MEMBER);
            }
        }
        return retrospectiveService.closeRetrospective(retro.getId());
    }

    /** Counts the tokens of the real model; the answers are untouched. */
    @TestConfiguration
    static class CountingConfig {

        @Bean
        @Primary
        CountingSummaryModel countingSummaryModel(@Qualifier("summaryModel") SummaryModel real) {
            return new CountingSummaryModel(real);
        }

    }

}
