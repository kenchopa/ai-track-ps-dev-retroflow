Feature: Unknown ids
  Every lookup of something that doesn't exist answers 404 with the entity and the id.
  (test-matrix row 32)

  @row-32
  Scenario Outline: <what> for an unknown id
    When I send <method> "<path>" with:
      """
      <body>
      """
    Then the request is refused with status 404
    And the error says "<error>"

    Examples:
      | what                           | method | path                                    | body                                   | error                             |
      | Close a retrospective          | PUT    | /api/retrospectives/{unknown}/close     | {}                                     | Retrospective {unknown}: not found |
      | Mark an action item as done    | PUT    | /api/action-items/{unknown}/complete    | {}                                     | ActionItem {unknown}: not found    |
      | Change a feedback item         | PUT    | /api/feedback-items/{unknown}           | {"content": "Changed"}                 | FeedbackItem {unknown}: not found  |
      | Change an action item          | PUT    | /api/action-items/{unknown}             | {"content": "Changed", "priority": "LOW"} | ActionItem {unknown}: not found    |
      | Add feedback                   | POST   | /api/retrospectives/{unknown}/feedback-items | {"content": "Hi", "type": "WENT_WELL", "author": "Mira Okafor"} | Retrospective {unknown}: not found |

  @row-32
  Scenario Outline: <what> for an unknown id
    When I send <method> "<path>"
    Then the request is refused with status 404
    And the error says "<error>"

    Examples:
      | what                          | method | path                                         | error                              |
      | Get a team                    | GET    | /api/teams/{unknown}                         | Team {unknown}: not found          |
      | Get a retrospective           | GET    | /api/retrospectives/{unknown}                | Retrospective {unknown}: not found |
      | List feedback                 | GET    | /api/retrospectives/{unknown}/feedback-items | Retrospective {unknown}: not found |
      | List a team's action items    | GET    | /api/teams/{unknown}/action-items            | Team {unknown}: not found          |
      | Delete a feedback item        | DELETE | /api/feedback-items/{unknown}                | FeedbackItem {unknown}: not found  |
      | Summarise a retrospective     | POST   | /api/retrospectives/{unknown}/summary        | Retrospective {unknown}: not found |
