Feature: One open retrospective per team
  A team runs one retrospective at a time. A new one can only start when the previous one is closed.
  (test-matrix rows 1-3 and 24-26, decisions R1-R3)

  Background:
    Given a team "Payments squad" with members "Mira Okafor, Tomas Brandt"

  @row-1
  Scenario: A second open retrospective is refused, and nothing is saved
    Given "Payments squad" has an open retrospective "Sprint 41"
    When I start a retrospective "Sprint 42" for "Payments squad"
    Then the request is refused with status 409
    And the error says "Team {Payments squad}: already has an open retrospective ({Sprint 41})"
    And "Payments squad" has 1 retrospective

  @row-2
  Scenario: After the open one is closed, a new one can start
    Given "Payments squad" has a closed retrospective "Sprint 40"
    And "Payments squad" has a closed retrospective "Sprint 41"
    When I start a retrospective "Sprint 42" for "Payments squad"
    Then the request succeeds with status 201
    And the new retrospective is OPEN and dated "today"
    And "Payments squad" has 3 retrospectives

  @row-3
  Scenario: The rule is per team: another team's open retrospective doesn't count
    Given a team "Platform guild" with members "Saskia de Vries"
    And "Platform guild" has an open retrospective "Guild sync"
    When I start a retrospective "Sprint 42" for "Payments squad"
    Then the request succeeds with status 201

  @row-24
  Scenario: A retrospective needs a title
    When I send POST "/api/teams/{Payments squad}/retrospectives" with:
      """
      {"title": " "}
      """
    Then the request is refused with status 400
    And the error mentions "title must not be blank"

  @row-24
  Scenario: A retrospective title has at most 200 characters
    When I start a retrospective with a title of 201 characters for "Payments squad"
    Then the request is refused with status 400
    And the error mentions "title size must be between 0 and 200"

  @row-25
  Scenario: Without a date, the retrospective is dated today
    When I send POST "/api/teams/{Payments squad}/retrospectives" with:
      """
      {"title": "Sprint 42"}
      """
    Then the request succeeds with status 201
    And the new retrospective is OPEN and dated "today"

  @row-25
  Scenario: A given date is kept
    When I send POST "/api/teams/{Payments squad}/retrospectives" with:
      """
      {"title": "Sprint 42", "date": "2026-10-23"}
      """
    Then the request succeeds with status 201
    And the new retrospective is OPEN and dated "2026-10-23"

  @row-26
  Scenario: A retrospective can't be started for a team that doesn't exist
    When I send POST "/api/teams/{unknown}/retrospectives" with:
      """
      {"title": "Sprint 42"}
      """
    Then the request is refused with status 404
    And the error says "Team {unknown}: not found"

  @row-26
  Scenario: The retrospectives of a team that doesn't exist can't be listed
    When I send GET "/api/teams/{unknown}/retrospectives"
    Then the request is refused with status 404
    And the error says "Team {unknown}: not found"

  Scenario: The retrospectives of a team are listed newest first
    Given "Payments squad" has a closed retrospective "Sprint 40"
    And "Payments squad" has a closed retrospective "Sprint 41"
    And "Payments squad" has an open retrospective "Sprint 42"
    When I list the retrospectives of "Payments squad"
    Then the request succeeds with status 200
    And the response lists the retrospectives "Sprint 42, Sprint 41, Sprint 40"
