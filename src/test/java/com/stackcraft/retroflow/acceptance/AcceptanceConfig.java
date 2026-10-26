package com.stackcraft.retroflow.acceptance;

import io.cucumber.spring.CucumberContextConfiguration;
import io.cucumber.spring.ScenarioScope;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;

/**
 * The application under test: the whole RetroFlow app on a random port, so every scenario goes
 * through HTTP, the controllers, the services and the database (StackCraft QA 1.1).
 * The summary model is scripted, so no scenario ever calls OpenAI. A summary takes at most
 * {@value #MAX_SUMMARY_ITEMS} items here, so the size limit can be tested with a few items.
 */
@CucumberContextConfiguration
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "retroflow.summary.max-items=" + AcceptanceConfig.MAX_SUMMARY_ITEMS)
@Import(AcceptanceConfig.Beans.class)
public class AcceptanceConfig {

    static final int MAX_SUMMARY_ITEMS = 6;

    /** Only loaded through @Import: @TestConfiguration classes are not picked up by component scanning. */
    @TestConfiguration
    static class Beans {

        @Bean
        @Primary
        ScriptedSummaryModel scriptedSummaryModel() {
            return new ScriptedSummaryModel();
        }

        /** A fresh world per scenario: ids, aliases and the last response never leak between scenarios. */
        @Bean
        @ScenarioScope
        World world(TestRestTemplate http) {
            return new World(http);
        }
    }
}
