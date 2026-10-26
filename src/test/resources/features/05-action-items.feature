Feature: Action items
  Marking an action item as done is one-way. A team sees its action items across all its
  retrospectives, filtered by priority and status.
  (test-matrix rows 15-18, 29, 34-35, decisions A1-A4)

  Background:
    Given a team "Payments squad" with members "Mira Okafor, Tomas Brandt"
    And "Payments squad" has an open retrospective "Sprint 42"
    And the retrospective "Sprint 42" has the items:
      | type        | content                | author       | priority | done |
      | ACTION_ITEM | Split the CI pipeline  | Tomas Brandt | HIGH     | no   |
      | ACTION_ITEM | Update the runbook     | Mira Okafor  | LOW      | yes  |

  @row-18
  Scenario: Mark an open action item as done
    When I mark the action item "Split the CI pipeline" as done
    Then the request succeeds with status 200
    And the action item "Split the CI pipeline" is done

  @row-16
  Scenario: Marking an action item as done twice is refused
    When I mark the action item "Update the runbook" as done
    Then the request is refused with status 409
    And the error says "ActionItem {Update the runbook}: already completed"

  @row-15
  Scenario: A done action item can't be undone
    When I try to undo the action item "Update the runbook"
    Then the request is refused with status 409
    And the error says "ActionItem {Update the runbook}: already completed, cannot be undone"
    And the action item "Update the runbook" is done

  @row-17
  Scenario: Undoing an action item that isn't done changes nothing
    When I try to undo the action item "Split the CI pipeline"
    Then the request succeeds with status 200
    And the action item "Split the CI pipeline" is open

  @row-29
  Scenario Outline: An action item needs a known priority
    When I send POST "/api/retrospectives/{Sprint 42}/action-items" with:
      """
      {"content": "Write the release notes", "author": "Mira Okafor"<priority>}
      """
    Then the request is refused with status 400
    And the error mentions "<reason>"

    Examples:
      | priority                | reason                                   |
      | , "priority": "URGENT"  | priority must be one of LOW, MEDIUM, HIGH |
      |                         | priority must not be blank               |

  @row-34
  Scenario Outline: A team's action items are filtered by priority and status, across all its retrospectives
    Given the retrospective "Sprint 42" has been closed
    And "Payments squad" has an open retrospective "Sprint 43"
    And the retrospective "Sprint 43" has the items:
      | type        | content                    | author       | priority | done |
      | ACTION_ITEM | Agree on a scope freeze    | Mira Okafor  | HIGH     | yes  |
      | ACTION_ITEM | Write a handover template  | Tomas Brandt | MEDIUM   | no   |
    And a team "Platform guild" with members "Saskia de Vries"
    And "Platform guild" has an open retrospective "Guild sync"
    And the retrospective "Guild sync" has the items:
      | type        | content                 | author          | priority | done |
      | ACTION_ITEM | Not ours to follow up   | Saskia de Vries | HIGH     | no   |
    When I list the action items of "Payments squad" with "<filter>"
    Then the request succeeds with status 200
    And the response lists exactly the action items "<items>"

    Examples:
      | filter                         | items                                                                                     |
      |                                | Split the CI pipeline, Update the runbook, Agree on a scope freeze, Write a handover template |
      | priority=HIGH                  | Split the CI pipeline, Agree on a scope freeze                                             |
      | completed=false                | Split the CI pipeline, Write a handover template                                          |
      | completed=true                 | Update the runbook, Agree on a scope freeze                                               |
      | priority=HIGH&completed=false  | Split the CI pipeline                                                                     |
      | priority=LOW&completed=false   |                                                                                           |

  @row-35
  Scenario: Filtering on an unknown priority is refused
    When I list the action items of "Payments squad" with "priority=URGENT"
    Then the request is refused with status 400
    And the error says "Team {Payments squad}: priority filter must be one of LOW, MEDIUM, HIGH"
