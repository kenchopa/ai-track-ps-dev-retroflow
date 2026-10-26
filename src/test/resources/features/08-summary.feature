Feature: AI summary of a closed retrospective
  A model summarises a closed retrospective. RetroFlow checks every answer against the feedback
  before anyone sees it, and explains every failure without provider details.
  (bonus challenge 2; the model is scripted here, nothing calls OpenAI)

  Background:
    Given a team "Payments squad" with members "Mira Okafor, Tomas Brandt"
    And "Payments squad" has an open retrospective "Sprint 42"
    And the retrospective "Sprint 42" has the items:
      | type              | content                    | author       | priority | done |
      | WENT_WELL         | Pairing on the refund flow | Mira Okafor  |          |      |
      | NEEDS_IMPROVEMENT | CI takes 25 minutes        | Tomas Brandt |          |      |
      | ACTION_ITEM       | Split the CI pipeline      | Tomas Brandt | HIGH     | no   |
      | ACTION_ITEM       | Update the runbook         | Mira Okafor  | LOW      | yes  |

  Scenario: Only a closed retrospective can be summarised
    When I ask for a summary of "Sprint 42"
    Then the request is refused with status 409
    And the error says "Retrospective {Sprint 42}: only a closed retrospective can be summarised"
    And the summary model was asked 0 times

  Scenario: A closed retrospective is summarised, with only the open action items
    Given the retrospective "Sprint 42" has been closed
    And the summary model writes a faithful summary
    When I ask for a summary of "Sprint 42"
    Then the request succeeds with status 200
    And the summary lists "Pairing on the refund flow" under "went well"
    And the summary lists "CI takes 25 minutes" under "needs improvement"
    And the summary lists the open action item "Split the CI pipeline" with priority HIGH
    And the summary does not list "Update the runbook" under "open action items"

  Scenario: A summary that drops every improvement is rejected (what a successful prompt injection looks like)
    Given the retrospective "Sprint 42" has been closed
    And the summary model leaves out every improvement
    When I ask for a summary of "Sprint 42"
    Then the request is refused with status 502
    And the error says "Retrospective {Sprint 42}: the summary service gave an answer we could not use, try again"

  Scenario Outline: Every way the model can fail gets its own status and message
    Given the retrospective "Sprint 42" has been closed
    And the summary model <behaviour>
    When I ask for a summary of "Sprint 42"
    Then the request is refused with status <status>
    And the error says "Retrospective {Sprint 42}: <reason>"

    Examples:
      | behaviour                         | status | reason                                                          |
      | answers with text instead of JSON | 502    | the summary service gave an answer we could not use, try again |
      | fails                             | 502    | the summary service failed, try again later                     |
      | times out                         | 504    | the summary took too long, try again later                      |
      | is rate limited                   | 503    | the summary service is busy, try again in a minute              |
      | is not configured                 | 503    | summaries are not available, the model is not configured        |

  Scenario: A rate limit tells the caller when to try again
    Given the retrospective "Sprint 42" has been closed
    And the summary model is rate limited
    When I ask for a summary of "Sprint 42"
    Then the request is refused with status 503
    And the response has the header "Retry-After" with value "60"

  Scenario: A retrospective with more items than a summary can take is refused before the model is asked
    Given the retrospective "Sprint 42" has the items:
      | type      | content          | author      | priority | done |
      | WENT_WELL | Demo landed well | Mira Okafor |          |      |
      | WENT_WELL | Reviews were fast | Mira Okafor |          |      |
      | WENT_WELL | Calm on-call week | Mira Okafor |          |      |
    And the retrospective "Sprint 42" has been closed
    When I ask for a summary of "Sprint 42"
    Then the request is refused with status 400
    And the error says "Retrospective {Sprint 42}: has 7 feedback items, a summary takes at most 6"
    And the summary model was asked 0 times
