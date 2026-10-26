Feature: A closed retrospective is frozen
  Once a retrospective is closed, nothing can be added, changed or deleted. Action items can still
  be marked as done: that is follow-up work after the retrospective.
  (test-matrix rows 6-14, decisions F3 and F6)

  Background:
    Given a team "Payments squad" with members "Mira Okafor, Tomas Brandt"
    And "Payments squad" has an open retrospective "Sprint 42"
    And the retrospective "Sprint 42" has the items:
      | type              | content                         | author       | priority |
      | WENT_WELL         | Pairing on the refund flow      | Mira Okafor  |          |
      | NEEDS_IMPROVEMENT | CI takes 25 minutes             | Tomas Brandt |          |
      | ACTION_ITEM       | Split the CI pipeline           | Tomas Brandt | HIGH     |

  @row-6
  Scenario: No feedback can be added to a closed retrospective
    Given the retrospective "Sprint 42" has been closed
    When "Mira Okafor" adds "Quick code reviews" as WENT_WELL to "Sprint 42"
    Then the request is refused with status 409
    And the error says "Retrospective {Sprint 42}: cannot add feedback to a closed retrospective"
    And "Sprint 42" has 3 items

  @row-7
  Scenario: No action item can be added to a closed retrospective
    Given the retrospective "Sprint 42" has been closed
    When "Mira Okafor" adds the action item "Write a runbook" with priority LOW to "Sprint 42"
    Then the request is refused with status 409
    And the error says "Retrospective {Sprint 42}: cannot add feedback to a closed retrospective"
    And "Sprint 42" has 3 items

  @row-8
  Scenario: Feedback in a closed retrospective can't be changed
    Given the retrospective "Sprint 42" has been closed
    When I change the feedback item "Pairing on the refund flow" to "Pairing was fine"
    Then the request is refused with status 409
    And the error says "FeedbackItem {Pairing on the refund flow}: cannot modify an item of a closed retrospective"
    And the item "Pairing on the refund flow" reads "Pairing on the refund flow"

  @row-9
  Scenario: Feedback in a closed retrospective can't be deleted
    Given the retrospective "Sprint 42" has been closed
    When I delete the item "CI takes 25 minutes"
    Then the request is refused with status 409
    And the error says "FeedbackItem {CI takes 25 minutes}: cannot delete an item of a closed retrospective"
    And the item "CI takes 25 minutes" still exists

  @row-10
  Scenario: An action item in a closed retrospective can't be changed
    Given the retrospective "Sprint 42" has been closed
    When I change the action item "Split the CI pipeline" to "Split the pipeline later" with priority LOW
    Then the request is refused with status 409
    And the error says "ActionItem {Split the CI pipeline}: cannot modify an item of a closed retrospective"
    And the item "Split the CI pipeline" reads "Split the CI pipeline"
    And the action item "Split the CI pipeline" has priority HIGH

  @row-11
  Scenario: An action item in a closed retrospective can't be deleted
    Given the retrospective "Sprint 42" has been closed
    When I delete the item "Split the CI pipeline"
    Then the request is refused with status 409
    And the error says "ActionItem {Split the CI pipeline}: cannot delete an item of a closed retrospective"
    And the item "Split the CI pipeline" still exists

  @row-12
  Scenario: An action item can still be marked as done after the retrospective is closed
    Given the retrospective "Sprint 42" has been closed
    When I mark the action item "Split the CI pipeline" as done
    Then the request succeeds with status 200
    And the action item "Split the CI pipeline" is done

  @row-13
  Scenario: While the retrospective is open, feedback can be changed
    When I change the feedback item "Pairing on the refund flow" to "Pairing caught two edge cases"
    Then the request succeeds with status 200
    And the item "Pairing on the refund flow" reads "Pairing caught two edge cases"

  @row-13
  Scenario: While the retrospective is open, an action item's text and priority can be changed
    When I change the action item "Split the CI pipeline" to "Split the CI pipeline in three jobs" with priority MEDIUM
    Then the request succeeds with status 200
    And the item "Split the CI pipeline" reads "Split the CI pipeline in three jobs"
    And the action item "Split the CI pipeline" has priority MEDIUM

  @row-14
  Scenario: While the retrospective is open, feedback can be deleted
    When I delete the item "CI takes 25 minutes"
    Then the request succeeds with status 204
    And the item "CI takes 25 minutes" is gone
    And "Sprint 42" has 2 items
