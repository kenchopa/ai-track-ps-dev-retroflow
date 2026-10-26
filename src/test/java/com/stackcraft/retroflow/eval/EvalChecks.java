package com.stackcraft.retroflow.eval;

import com.stackcraft.retroflow.service.RetrospectiveSummary;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Everything code can decide about a summary, decided by code: did it come
 * back, does each section mention what it must, does it avoid what it must
 * not say, is it short enough, is it in the right language. Same summary in,
 * same verdict out.
 */
final class EvalChecks {

    /** Common Dutch words; at least one per point means the point is Dutch. Crude, but code. */
    private static final Set<String> DUTCH = Set.of("de", "het", "een", "en", "van", "is", "te", "niet", "op",
            "werkte", "duurt", "lang", "elke", "goed", "bij", "met", "voor", "in", "aan");

    record Check(String name, boolean passed, String detail) {
    }

    private EvalChecks() {
    }

    /** The checks for a summary that came back. */
    static List<Check> check(EvalCase evalCase, RetrospectiveSummary summary) {
        List<Check> checks = new ArrayList<>();
        checks.add(new Check("valid answer", true, "summary returned and accepted by the validator"));
        EvalCase.Expect expect = evalCase.expect();
        expect.mentionsOrEmpty().forEach((section, words) -> checks.add(mentions(section, points(summary, section), words)));
        for (String word : expect.forbiddenOrEmpty()) {
            List<String> hits = all(summary).filter(point -> contains(point, word)).toList();
            checks.add(new Check("never says '" + word + "'", hits.isEmpty(), hits.isEmpty() ? "" : hits.toString()));
        }
        expect.maxPointsOrEmpty().forEach((section, max) -> {
            int size = points(summary, section).size();
            checks.add(new Check(section + " has at most " + max + " points", size <= max, size + " points"));
        });
        if ("nl".equals(expect.language())) {
            List<String> notDutch = all(summary).filter(point -> !isDutch(point)).toList();
            checks.add(new Check("written in Dutch", notDutch.isEmpty(), notDutch.isEmpty() ? "" : notDutch.toString()));
        }
        return checks;
    }

    /** The checks for a summary that did not come back: the first one fails, the others can't pass. */
    static List<Check> failed(EvalCase evalCase, String reason) {
        List<Check> checks = new ArrayList<>(List.of(new Check("valid answer", false, reason)));
        evalCase.expect().mentionsOrEmpty().keySet().forEach(section -> checks.add(new Check("mentions in " + section, false, "no summary")));
        evalCase.expect().forbiddenOrEmpty().forEach(word -> checks.add(new Check("never says '" + word + "'", false, "no summary")));
        evalCase.expect().maxPointsOrEmpty().keySet().forEach(section -> checks.add(new Check(section + " length", false, "no summary")));
        if ("nl".equals(evalCase.expect().language())) {
            checks.add(new Check("written in Dutch", false, "no summary"));
        }
        return checks;
    }

    private static Check mentions(String section, List<String> points, List<String> words) {
        boolean found = points.stream().anyMatch(point -> words.stream().anyMatch(word -> contains(point, word)));
        return new Check(section + " mentions " + words, found, found ? "" : points.toString());
    }

    static List<String> points(RetrospectiveSummary summary, String section) {
        return switch (section) {
            case "wentWell" -> summary.wentWell();
            case "needsImprovement" -> summary.needsImprovement();
            case "openActionItems" -> summary.openActionItems().stream().map(RetrospectiveSummary.OpenActionItem::summary).toList();
            default -> throw new IllegalArgumentException("unknown section " + section);
        };
    }

    private static Stream<String> all(RetrospectiveSummary summary) {
        return Stream.of("wentWell", "needsImprovement", "openActionItems").flatMap(section -> points(summary, section).stream());
    }

    private static boolean contains(String point, String word) {
        return point.toLowerCase(Locale.ROOT).contains(word.toLowerCase(Locale.ROOT));
    }

    private static boolean isDutch(String point) {
        return Stream.of(point.toLowerCase(Locale.ROOT).split("[^\\p{L}]+")).anyMatch(DUTCH::contains);
    }

    static Map<String, Object> asMap(Check check) {
        return Map.of("check", check.name(), "passed", check.passed(), "detail", check.detail());
    }

}
