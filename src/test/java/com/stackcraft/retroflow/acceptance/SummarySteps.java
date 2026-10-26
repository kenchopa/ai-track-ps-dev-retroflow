package com.stackcraft.retroflow.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.java.ParameterType;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** The AI summary of a closed retrospective, with the model scripted per scenario. */
public class SummarySteps {

    private final World world;
    private final ScriptedSummaryModel model;

    public SummarySteps(World world, ScriptedSummaryModel model) {
        this.world = world;
        this.model = model;
    }

    @ParameterType("writes a faithful summary|leaves out every improvement|answers with text instead of JSON|times out|is rate limited|fails|is not configured")
    public ScriptedSummaryModel.Behaviour behaviour(String phrase) {
        return switch (phrase) {
            case "writes a faithful summary" -> ScriptedSummaryModel.Behaviour.FAITHFUL;
            case "leaves out every improvement" -> ScriptedSummaryModel.Behaviour.LEAVES_OUT_IMPROVEMENTS;
            case "answers with text instead of JSON" -> ScriptedSummaryModel.Behaviour.NOT_JSON;
            case "times out" -> ScriptedSummaryModel.Behaviour.TIMES_OUT;
            case "is rate limited" -> ScriptedSummaryModel.Behaviour.RATE_LIMITED;
            case "fails" -> ScriptedSummaryModel.Behaviour.FAILS;
            default -> ScriptedSummaryModel.Behaviour.NOT_CONFIGURED;
        };
    }

    @Given("the summary model {behaviour}")
    public void modelBehaves(ScriptedSummaryModel.Behaviour behaviour) {
        model.behave(behaviour);
    }

    @When("I ask for a summary of {string}")
    public void askForSummary(String retro) {
        world.send(HttpMethod.POST, "/api/retrospectives/" + world.id(retro) + "/summary", null);
    }

    @Then("the summary lists {string} under {string}")
    public void lists(String text, String section) {
        assertThat(section(section)).contains(text);
    }

    @Then("the summary does not list {string} under {string}")
    public void doesNotList(String text, String section) {
        assertThat(section(section)).doesNotContain(text);
    }

    @Then("the summary has nothing under {string}")
    public void empty(String section) {
        assertThat(section(section)).isEmpty();
    }

    @Then("the summary lists the open action item {string} with priority {word}")
    public void openAction(String content, String priority) {
        JsonNode match = null;
        for (JsonNode item : world.last().body().path("openActionItems")) {
            if (item.path("summary").asText().equals(content)) {
                match = item;
            }
        }
        assertThat(match).as("open action item '%s' in %s", content, world.last().body()).isNotNull();
        assertThat(match.path("id").asLong()).isEqualTo(world.id(content));
        assertThat(match.path("priority").asText()).isEqualTo(priority);
    }

    @Then("the summary model was asked {int} time(s)")
    public void asked(int times) {
        assertThat(model.calls()).isEqualTo(times);
    }

    private List<String> section(String name) {
        String field = switch (name) {
            case "went well" -> "wentWell";
            case "needs improvement" -> "needsImprovement";
            case "open action items" -> "openActionItems";
            default -> throw new IllegalArgumentException("unknown summary section '" + name + "'");
        };
        List<String> texts = new ArrayList<>();
        world.last().body().path(field).forEach(node -> texts.add(node.isTextual() ? node.asText() : node.path("summary").asText()));
        return texts;
    }
}
