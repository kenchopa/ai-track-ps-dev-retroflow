package com.stackcraft.retroflow.acceptance;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpMethod;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Starting, listing and closing retrospectives. */
public class RetrospectiveSteps {

    private final World world;

    public RetrospectiveSteps(World world) {
        this.world = world;
    }

    @Given("{string} has an open retrospective {string}")
    public void openRetro(String team, String title) {
        World.Response response = world.require(HttpMethod.POST, "/api/teams/" + world.id(team) + "/retrospectives", Map.of("title", title));
        world.remember(title, response.body().path("id").asLong());
    }

    @Given("{string} has a closed retrospective {string}")
    public void closedRetro(String team, String title) {
        openRetro(team, title);
        world.require(HttpMethod.PUT, "/api/retrospectives/" + world.id(title) + "/close", null);
    }

    @Given("the retrospective {string} has been closed")
    public void hasBeenClosed(String title) {
        world.require(HttpMethod.PUT, "/api/retrospectives/" + world.id(title) + "/close", null);
    }

    @When("I start a retrospective {string} for {string}")
    public void start(String title, String team) {
        World.Response response = world.send(HttpMethod.POST, "/api/teams/" + world.id(team) + "/retrospectives", Map.of("title", title));
        if (response.status() == 201) {
            world.remember(title, response.body().path("id").asLong());
        }
    }

    @When("I start a retrospective with a title of {int} characters for {string}")
    public void startWithLongTitle(int length, String team) {
        world.send(HttpMethod.POST, "/api/teams/" + world.id(team) + "/retrospectives", Map.of("title", "x".repeat(length)));
    }

    @When("I close the retrospective {string}")
    public void close(String title) {
        world.send(HttpMethod.PUT, "/api/retrospectives/" + world.id(title) + "/close", null);
    }

    @When("I try to reopen the retrospective {string}")
    public void reopen(String title) {
        world.send(HttpMethod.PUT, "/api/retrospectives/" + world.id(title) + "/reopen", null);
    }

    @When("I list the retrospectives of {string}")
    public void list(String team) {
        world.send(HttpMethod.GET, "/api/teams/" + world.id(team) + "/retrospectives", null);
    }

    @Then("{string} has {int} retrospective(s)")
    public void count(String team, int expected) {
        assertThat(world.require(HttpMethod.GET, "/api/teams/" + world.id(team) + "/retrospectives", null).body()).hasSize(expected);
    }

    @Then("the retrospective {string} is {word}")
    public void status(String title, String status) {
        assertThat(world.require(HttpMethod.GET, "/api/retrospectives/" + world.id(title), null).body().path("status").asText()).isEqualTo(status);
    }

    @Then("the new retrospective is {word} and dated {string}")
    public void newRetro(String status, String date) {
        World.Response response = world.last();
        String expectedDate = date.equals("today") ? LocalDate.now().toString() : date;
        assertThat(response.body().path("status").asText()).isEqualTo(status);
        assertThat(response.body().path("date").asText()).isEqualTo(expectedDate);
        assertThat(response.headers().getLocation()).hasToString("/api/retrospectives/" + response.body().path("id").asLong());
    }

    @Then("the response lists the retrospectives {string}")
    public void listed(String titles) {
        assertThat(world.last().body().findValuesAsText("title")).containsExactlyElementsOf(TeamSteps.members(titles));
    }
}
