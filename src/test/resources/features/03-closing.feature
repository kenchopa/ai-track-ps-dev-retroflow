Feature: Closing a retrospective
  Closing is one-way: a closed retrospective can't be closed again or reopened.
  (test-matrix rows 4-5, decisions R4-R5)

  Background:
    Given a team "Payments squad" with members "Mira Okafor"
    And "Payments squad" has an open retrospective "Sprint 42"

  Scenario: Close an open retrospective
    When I close the retrospective "Sprint 42"
    Then the request succeeds with status 200
    And the retrospective "Sprint 42" is CLOSED

  @row-4
  Scenario: Closing twice is refused, and it stays closed
    Given the retrospective "Sprint 42" has been closed
    When I close the retrospective "Sprint 42"
    Then the request is refused with status 409
    And the error says "Retrospective {Sprint 42}: already closed"
    And the retrospective "Sprint 42" is CLOSED

  @row-5
  Scenario: There is no way to reopen a closed retrospective
    Given the retrospective "Sprint 42" has been closed
    When I try to reopen the retrospective "Sprint 42"
    Then the request is refused with status 404
    And the retrospective "Sprint 42" is CLOSED
