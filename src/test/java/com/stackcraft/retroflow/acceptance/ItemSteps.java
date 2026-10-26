package com.stackcraft.retroflow.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import io.cucumber.datatable.DataTable;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/** Feedback items and action items: adding, changing, deleting, completing and listing them. */
public class ItemSteps {

    private final World world;

    public ItemSteps(World world) {
        this.world = world;
    }

    /**
     * | type | content | author | priority | done |, with priority and done only for ACTION_ITEM.
     * The items are added through the API while the retrospective is still open.
     */
    @Given("the retrospective {string} has the items:")
    public void items(String retro, DataTable table) {
        for (Map<String, String> row : table.asMaps()) {
            String type = row.get("type");
            String content = row.get("content");
            String author = row.get("author");
            if ("ACTION_ITEM".equals(type)) {
                World.Response added = world.require(HttpMethod.POST, "/api/retrospectives/" + world.id(retro) + "/action-items",
                        Map.of("content", content, "priority", row.get("priority"), "author", author));
                world.rememberItem(content, added.body().path("id").asLong(), retro);
                if ("yes".equals(row.get("done"))) {
                    world.require(HttpMethod.PUT, "/api/action-items/" + world.id(content) + "/complete", null);
                }
            } else {
                World.Response added = world.require(HttpMethod.POST, "/api/retrospectives/" + world.id(retro) + "/feedback-items",
                        Map.of("content", content, "type", type, "author", author));
                world.rememberItem(content, added.body().path("id").asLong(), retro);
            }
        }
    }

    @When("{string} adds {string} as {word} to {string}")
    public void addFeedback(String author, String content, String type, String retro) {
        World.Response response = world.send(HttpMethod.POST, "/api/retrospectives/" + world.id(retro) + "/feedback-items",
                Map.of("content", content, "type", type, "author", author));
        if (response.status() == 201) {
            world.rememberItem(content, response.body().path("id").asLong(), retro);
        }
    }

    @When("{string} adds the action item {string} with priority {word} to {string}")
    public void addAction(String author, String content, String priority, String retro) {
        World.Response response = world.send(HttpMethod.POST, "/api/retrospectives/" + world.id(retro) + "/action-items",
                Map.of("content", content, "priority", priority, "author", author));
        if (response.status() == 201) {
            world.rememberItem(content, response.body().path("id").asLong(), retro);
        }
    }

    @When("{string} adds a text of {int} characters as {word} to {string}")
    public void addLongFeedback(String author, int length, String type, String retro) {
        world.send(HttpMethod.POST, "/api/retrospectives/" + world.id(retro) + "/feedback-items",
                Map.of("content", "x".repeat(length), "type", type, "author", author));
    }

    @When("{string} adds an action item of {int} characters to {string}")
    public void addLongAction(String author, int length, String retro) {
        world.send(HttpMethod.POST, "/api/retrospectives/" + world.id(retro) + "/action-items",
                Map.of("content", "x".repeat(length), "priority", "LOW", "author", author));
    }

    @When("I change the feedback item {string} to {string}")
    public void changeFeedback(String item, String content) {
        world.send(HttpMethod.PUT, "/api/feedback-items/" + world.id(item), Map.of("content", content));
    }

    @When("I change the action item {string} to {string} with priority {word}")
    public void changeAction(String item, String content, String priority) {
        world.send(HttpMethod.PUT, "/api/action-items/" + world.id(item), Map.of("content", content, "priority", priority));
    }

    @When("I delete the item {string}")
    public void delete(String item) {
        world.send(HttpMethod.DELETE, "/api/feedback-items/" + world.id(item), null);
    }

    @When("I mark the action item {string} as done")
    public void complete(String item) {
        world.send(HttpMethod.PUT, "/api/action-items/" + world.id(item) + "/complete", null);
    }

    @When("I try to undo the action item {string}")
    public void uncomplete(String item) {
        world.send(HttpMethod.PUT, "/api/action-items/" + world.id(item) + "/uncomplete", null);
    }

    @When("I list the feedback of {string}")
    public void listFeedback(String retro) {
        world.send(HttpMethod.GET, "/api/retrospectives/" + world.id(retro) + "/feedback-items", null);
    }

    @When("I list the action items of {string}")
    public void listActions(String team) {
        world.send(HttpMethod.GET, "/api/teams/" + world.id(team) + "/action-items", null);
    }

    @When("I list the action items of {string} with {string}")
    public void listActionsFiltered(String team, String query) {
        String suffix = query.isBlank() ? "" : "?" + query;
        world.send(HttpMethod.GET, "/api/teams/" + world.id(team) + "/action-items" + suffix, null);
    }

    @Then("the response lists the items {string}")
    public void listedInOrder(String contents) {
        assertThat(world.last().body().findValuesAsText("content")).containsExactlyElementsOf(TeamSteps.members(contents));
    }

    @Then("the response lists exactly the action items {string}")
    public void listedAnyOrder(String contents) {
        assertThat(world.last().body().findValuesAsText("content")).containsExactlyInAnyOrderElementsOf(TeamSteps.members(contents));
    }

    @Then("in the response, {string} is a {word} without priority or completion")
    public void plainItem(String content, String type) {
        JsonNode item = inResponse(content);
        assertThat(item.path("type").asText()).isEqualTo(type);
        assertThat(item.has("priority")).as("priority on a plain feedback item").isFalse();
        assertThat(item.has("completed")).as("completed on a plain feedback item").isFalse();
    }

    @Then("in the response, {string} is an action item with priority {word} that is {word}")
    public void actionInResponse(String content, String priority, String state) {
        JsonNode item = inResponse(content);
        assertThat(item.path("type").asText()).isEqualTo("ACTION_ITEM");
        assertThat(item.path("priority").asText()).isEqualTo(priority);
        assertThat(item.path("completed").asBoolean()).isEqualTo("done".equals(state));
    }

    @Then("the item {string} reads {string}")
    public void reads(String item, String content) {
        assertThat(stored(item)).as("item '%s'", item).hasValueSatisfying(node -> assertThat(node.path("content").asText()).isEqualTo(content));
    }

    @Then("the item {string} still exists")
    public void stillExists(String item) {
        assertThat(stored(item)).as("item '%s'", item).isPresent();
    }

    @Then("the item {string} is gone")
    public void gone(String item) {
        assertThat(stored(item)).as("item '%s'", item).isEmpty();
    }

    @Then("the action item {string} is {word}")
    public void actionState(String item, String state) {
        assertThat(stored(item)).as("action item '%s'", item)
                .hasValueSatisfying(node -> assertThat(node.path("completed").asBoolean()).isEqualTo("done".equals(state)));
    }

    @Then("the action item {string} has priority {word}")
    public void priority(String item, String priority) {
        assertThat(stored(item)).as("action item '%s'", item)
                .hasValueSatisfying(node -> assertThat(node.path("priority").asText()).isEqualTo(priority));
    }

    @Then("{string} has {int} item(s)")
    public void itemCount(String retro, int expected) {
        assertThat(world.require(HttpMethod.GET, "/api/retrospectives/" + world.id(retro) + "/feedback-items", null).body()).hasSize(expected);
    }

    /** The item as the API lists it now (looked up through its retrospective), or empty if it is gone. */
    private Optional<JsonNode> stored(String item) {
        long id = world.id(item);
        String retro = world.retroOf(item);
        JsonNode items = world.require(HttpMethod.GET, "/api/retrospectives/" + world.id(retro) + "/feedback-items", null).body();
        List<JsonNode> matches = new ArrayList<>();
        items.forEach(node -> {
            if (node.path("id").asLong() == id) {
                matches.add(node);
            }
        });
        return matches.stream().findFirst();
    }

    private JsonNode inResponse(String content) {
        for (JsonNode node : world.last().body()) {
            if (node.path("content").asText().equals(content)) {
                return node;
            }
        }
        throw new AssertionError("'" + content + "' is not in the response " + world.last().body());
    }
}
