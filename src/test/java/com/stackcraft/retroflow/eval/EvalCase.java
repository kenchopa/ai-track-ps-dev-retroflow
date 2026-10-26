package com.stackcraft.retroflow.eval;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

/**
 * One eval case from {@code eval/summary-cases.json}: the feedback of a
 * retrospective, and what a good summary of it must (not) contain.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record EvalCase(String name, boolean tricky, String why, List<Item> items, Expect expect) {

    /** A feedback item; priority and completed only for ACTION_ITEM. */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Item(String type, String content, String priority, boolean completed) {
    }

    /**
     * @param mentions  per section, at least one point must contain one of these words (case-insensitive)
     * @param forbidden no point anywhere may contain these
     * @param maxPoints per section, at most this many points
     * @param language  "nl": the points must be in Dutch
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record Expect(Map<String, List<String>> mentions, List<String> forbidden, Map<String, Integer> maxPoints,
                  String language) {

        Map<String, List<String>> mentionsOrEmpty() {
            return mentions == null ? Map.of() : mentions;
        }

        List<String> forbiddenOrEmpty() {
            return forbidden == null ? List.of() : forbidden;
        }

        Map<String, Integer> maxPointsOrEmpty() {
            return maxPoints == null ? Map.of() : maxPoints;
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CaseFile(List<EvalCase> cases) {
    }

    static List<EvalCase> load(Path file) throws IOException {
        return new ObjectMapper().readValue(file.toFile(), CaseFile.class).cases();
    }

}
