package com.stackcraft.retroflow.eval;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Turns the results into one score and a cost, prints them, and writes
 * {@code eval/results/<label>.json} so two runs can be compared.
 */
final class EvalReport {

    /** USD per 1M tokens, input and output: OpenAI pricing page, checked 2026-10-26. */
    private static final Map<String, double[]> USD_PER_MILLION = Map.of(
            "gpt-4.1", new double[]{2.00, 8.00},
            "gpt-4.1-mini", new double[]{0.40, 1.60},
            "gpt-4.1-nano", new double[]{0.10, 0.40});
    /** ECB reference rate of 2026-10-23: 1 EUR = 1.1193 USD. */
    private static final double EUR_PER_USD = 1 / 1.1193;

    /** {@code summary} is null when the model or the validator gave none. */
    record CaseResult(String name, boolean tricky, List<EvalChecks.Check> checks, Object summary) {

        long passed() {
            return checks.stream().filter(EvalChecks.Check::passed).count();
        }
    }

    record Usage(String model, long inputTokens, long outputTokens) {

        double usd() {
            double[] price = USD_PER_MILLION.getOrDefault(model, new double[]{Double.NaN, Double.NaN});
            return (inputTokens * price[0] + outputTokens * price[1]) / 1_000_000;
        }
    }

    private EvalReport() {
    }

    static double score(List<CaseResult> results) {
        long passed = results.stream().mapToLong(CaseResult::passed).sum();
        long total = results.stream().mapToLong(result -> result.checks().size()).sum();
        return total == 0 ? 0 : 100.0 * passed / total;
    }

    static String print(String label, List<CaseResult> results, Usage candidate, Usage judge, long millis) {
        StringBuilder out = new StringBuilder("\n=== Summary eval: " + label + " ===\n");
        for (CaseResult result : results) {
            out.append(String.format(Locale.ROOT, "%-28s %2d/%-2d %s%n", result.name() + (result.tricky() ? " *" : ""),
                    result.passed(), result.checks().size(), failures(result)));
        }
        long fullPasses = results.stream().filter(result -> result.passed() == result.checks().size()).count();
        out.append(String.format(Locale.ROOT, "%nSCORE %.1f%%  (%d of %d cases fully passed, * = tricky)%n",
                score(results), fullPasses, results.size()));
        out.append(cost("summaries", candidate)).append(cost("judge", judge));
        out.append(String.format(Locale.ROOT, "total  $%.4f = EUR %.4f per run, %.1f s%n",
                candidate.usd() + judge.usd(), (candidate.usd() + judge.usd()) * EUR_PER_USD, millis / 1000.0));
        return out.toString();
    }

    static void write(Path file, String label, Map<String, Object> settings, List<CaseResult> results,
                      Usage candidate, Usage judge) throws IOException {
        Map<String, Object> report = new LinkedHashMap<>();
        report.put("label", label);
        report.put("settings", settings);
        report.put("score", Math.round(score(results) * 10) / 10.0);
        report.put("usage", Map.of("summaries", candidate, "judge", judge,
                "usd", candidate.usd() + judge.usd(), "eur", (candidate.usd() + judge.usd()) * EUR_PER_USD));
        report.put("cases", results.stream().map(EvalReport::asMap).toList());
        Files.createDirectories(file.getParent());
        new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT).writeValue(file.toFile(), report);
    }

    private static Map<String, Object> asMap(CaseResult result) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("name", result.name());
        map.put("passed", result.passed());
        map.put("of", result.checks().size());
        map.put("checks", result.checks().stream().map(EvalChecks::asMap).toList());
        map.put("summary", result.summary());
        return map;
    }

    private static String failures(CaseResult result) {
        return result.checks().stream().filter(check -> !check.passed())
                .map(check -> "FAIL " + check.name() + (check.detail().isBlank() ? "" : ": " + check.detail()))
                .reduce((a, b) -> a + " | " + b).orElse("");
    }

    private static String cost(String what, Usage usage) {
        return String.format(Locale.ROOT, "%-9s %-12s %,8d in %,7d out  $%.4f%n",
                what, usage.model(), usage.inputTokens(), usage.outputTokens(), usage.usd());
    }

}
