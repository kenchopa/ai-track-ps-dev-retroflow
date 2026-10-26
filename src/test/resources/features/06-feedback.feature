Feature: Feedback
  Team members say what went well and what needs to improve. Authors must be members of the team.
  (test-matrix rows 27-28, 30-31 and 33, decisions F1-F2, F4-F5, F7)

  Background:
    Given a team "Payments squad" with members "Mira Okafor, Tomas Brandt"
    And "Payments squad" has an open retrospective "Sprint 42"

  Scenario: A member adds what went well
    When "Mira Okafor" adds "Pairing on the refund flow" as WENT_WELL to "Sprint 42"
    Then the request succeeds with status 201
    And "Sprint 42" has 1 item

  @row-27
  Scenario Outline: Feedback has one of two known types, written exactly
    When I send POST "/api/retrospectives/{Sprint 42}/feedback-items" with:
      """
      {"content": "Good sprint", "type": "<type>", "author": "Mira Okafor"}
      """
    Then the request is refused with status 400
    And the error says "FeedbackItem new: type must be one of WENT_WELL, NEEDS_IMPROVEMENT"

    Examples:
      | type      |
      | GREAT     |
      | went_well |

  @row-28
  Scenario: An action item can't be added as plain feedback, because it needs a priority
    When "Mira Okafor" adds "Split the CI pipeline" as ACTION_ITEM to "Sprint 42"
    Then the request is refused with status 400
    And the error says "FeedbackItem new: type ACTION_ITEM must be added as an action item"

  @row-30
  Scenario: Only members of the team can add feedback
    When "Zoe Lindqvist" adds "Nice demo" as WENT_WELL to "Sprint 42"
    Then the request is refused with status 400
    And the error says "Team {Payments squad}: 'Zoe Lindqvist' is not a member"
    And "Sprint 42" has 0 items

  @row-30
  Scenario: Members are recognised without regard to case
    When "mira okafor" adds "Nice demo" as WENT_WELL to "Sprint 42"
    Then the request succeeds with status 201

  @row-30
  Scenario: Only members of the team can add action items
    When "Zoe Lindqvist" adds the action item "Book the offsite" with priority LOW to "Sprint 42"
    Then the request is refused with status 400
    And the error says "Team {Payments squad}: 'Zoe Lindqvist' is not a member"

  @row-31
  Scenario Outline: Feedback and action items need content
    When I send POST "/api/retrospectives/{Sprint 42}/<endpoint>" with:
      """
      {"content": " ", "type": "WENT_WELL", "priority": "LOW", "author": "Mira Okafor"}
      """
    Then the request is refused with status 400
    And the error mentions "content must not be blank"

    Examples:
      | endpoint       |
      | feedback-items |
      | action-items   |

  @row-31
  Scenario: Feedback has at most 2000 characters
    When "Mira Okafor" adds a text of 2001 characters as WENT_WELL to "Sprint 42"
    Then the request is refused with status 400
    And the error mentions "content size must be between 0 and 2000"
    And "Sprint 42" has 0 items

  @row-31
  Scenario: An action item has at most 2000 characters
    When "Mira Okafor" adds an action item of 2001 characters to "Sprint 42"
    Then the request is refused with status 400
    And the error mentions "content size must be between 0 and 2000"

  @row-33
  Scenario: The feedback of a retrospective lists every item, action items with their priority and status
    Given the retrospective "Sprint 42" has the items:
      | type              | content                    | author       | priority | done |
      | WENT_WELL         | Pairing on the refund flow | Mira Okafor  |          |      |
      | NEEDS_IMPROVEMENT | CI takes 25 minutes        | Tomas Brandt |          |      |
      | ACTION_ITEM       | Split the CI pipeline      | Tomas Brandt | HIGH     | no   |
      | ACTION_ITEM       | Update the runbook         | Mira Okafor  | LOW      | yes  |
    When I list the feedback of "Sprint 42"
    Then the request succeeds with status 200
    And the response lists the items "Pairing on the refund flow, CI takes 25 minutes, Split the CI pipeline, Update the runbook"
    And in the response, "Pairing on the refund flow" is a WENT_WELL without priority or completion
    And in the response, "CI takes 25 minutes" is a NEEDS_IMPROVEMENT without priority or completion
    And in the response, "Split the CI pipeline" is an action item with priority HIGH that is open
    And in the response, "Update the runbook" is an action item with priority LOW that is done
