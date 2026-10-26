package com.stackcraft.retroflow.acceptance;

import io.cucumber.java.Before;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import org.springframework.http.HttpMethod;

import static org.assertj.core.api.Assertions.assertThat;

/** Status codes, error messages and raw requests: the parts every feature shares. */
public class HttpSteps {

    private final World world;
    private final ScriptedSummaryModel model;

    public HttpSteps(World world, ScriptedSummaryModel model) {
        this.world = world;
        this.model = model;
    }

    @Before
    public void resetModel() {
        model.reset();
    }

    @When("I send {word} {string}")
    public void send(String method, String path) {
        world.send(HttpMethod.valueOf(method), world.resolve(path), null);
    }

    @When("I send {word} {string} with:")
    public void sendWith(String method, String path, String body) {
        world.send(HttpMethod.valueOf(method), world.resolve(path), world.resolve(body));
    }

    @Then("the request succeeds with status {int}")
    public void succeeds(int status) {
        World.Response response = world.last();
        assertThat(response.status()).as("response body %s", response.body()).isEqualTo(status);
    }

    @Then("the request is refused with status {int}")
    public void refused(int status) {
        World.Response response = world.last();
        assertThat(response.status()).as("response body %s", response.body()).isEqualTo(status);
        assertThat(response.body().path("status").asInt()).as("status in the error body (standard 2.2)").isEqualTo(status);
        assertThat(response.body().path("timestamp").asText()).as("timestamp in the error body").isNotBlank();
    }

    @Then("the error says {string}")
    public void errorSays(String expected) {
        assertThat(world.last().error()).isEqualTo(world.resolve(expected));
    }

    @Then("the error mentions {string}")
    public void errorMentions(String expected) {
        assertThat(world.last().error()).contains(world.resolve(expected));
    }

    @Then("the response has the header {string} with value {string}")
    public void header(String name, String value) {
        assertThat(world.last().headers().getFirst(name)).isEqualTo(value);
    }
}
