package com.stackcraft.retroflow.acceptance;

import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpMethod;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Teams: creating them, and what a created team looks like. */
public class TeamSteps {

    private final World world;

    public TeamSteps(World world) {
        this.world = world;
    }

    /** "Mira, Tomas" becomes ["Mira", "Tomas"]; an empty text becomes no members. */
    static List<String> members(String names) {
        return names.isBlank() ? List.of() : Arrays.stream(names.split(",")).map(String::strip).toList();
    }

    @Given("a team {string} with members {string}")
    public void aTeam(String alias, String names) {
        World.Response response = world.require(HttpMethod.POST, "/api/teams", Map.of("name", world.teamName(alias), "members", members(names)));
        world.remember(alias, response.body().path("id").asLong());
    }

    @When("I create a team {string} with members {string}")
    public void createTeam(String alias, String names) {
        World.Response response = world.send(HttpMethod.POST, "/api/teams", Map.of("name", world.teamName(alias), "members", members(names)));
        if (response.status() == 201) {
            world.remember(alias, response.body().path("id").asLong());
        }
    }

    @When("I create a team with a name of {int} characters")
    public void createTeamWithLongName(int length) {
        world.send(HttpMethod.POST, "/api/teams", Map.of("name", "x".repeat(length), "members", List.of("Mira Okafor")));
    }

    @Then("the new team is called {string} with members {string}")
    public void newTeam(String alias, String names) {
        World.Response response = world.last();
        assertThat(response.body().path("name").asText()).isEqualTo(world.teamName(alias));
        List<String> actual = new ArrayList<>();
        response.body().path("members").forEach(member -> actual.add(member.asText()));
        assertThat(actual).containsExactlyElementsOf(members(names));
        assertThat(response.headers().getLocation()).hasToString("/api/teams/" + response.body().path("id").asLong());
    }

    @Then("there is no team called {string}")
    public void noTeam(String alias) {
        World.Response teams = world.require(HttpMethod.GET, "/api/teams", null);
        List<String> names = new ArrayList<>();
        teams.body().forEach(team -> names.add(team.path("name").asText()));
        assertThat(names).doesNotContain(world.teamName(alias));
    }
}
