Feature: Teams
  A team holds retrospectives together. Everyone who adds feedback must be a member.
  (test-matrix rows 19-23, decisions T1-T3)

  Scenario: Create a team with its members
    When I create a team "Payments squad" with members "Mira Okafor, Tomas Brandt"
    Then the request succeeds with status 201
    And the new team is called "Payments squad" with members "Mira Okafor, Tomas Brandt"

  @row-19
  Scenario: A team needs at least one member
    When I create a team "Payments squad" with members ""
    Then the request is refused with status 400
    And the error says "Team new: members must not be empty"
    And there is no team called "Payments squad"

  @row-19
  Scenario: A team without a members list is refused too
    When I send POST "/api/teams" with:
      """
      {"name": "Lone wolves"}
      """
    Then the request is refused with status 400
    And the error says "Team new: members must not be empty"

  @row-20
  Scenario: A team needs a name
    When I send POST "/api/teams" with:
      """
      {"name": "  ", "members": ["Mira Okafor"]}
      """
    Then the request is refused with status 400
    And the error mentions "name must not be blank"

  @row-20
  Scenario: A team name has at most 100 characters
    When I create a team with a name of 101 characters
    Then the request is refused with status 400
    And the error mentions "name size must be between 0 and 100"

  @row-21
  Scenario: A member name can't be blank
    When I send POST "/api/teams" with:
      """
      {"name": "Blank names", "members": ["Mira Okafor", "  "]}
      """
    Then the request is refused with status 400
    And the error mentions "must not be blank"

  @row-22
  Scenario: The same person can't be listed twice, whatever the case
    When I create a team "Payments squad" with members "Mira Okafor, mira okafor"
    Then the request is refused with status 400
    And the error says "Team new: member 'mira okafor' is listed more than once"

  @row-23
  Scenario: Team names are unique, ignoring case
    Given a team "Payments squad" with members "Mira Okafor"
    When I create a team "PAYMENTS SQUAD" with members "Tomas Brandt"
    Then the request is refused with status 409
    And the error says "Team new: a team named '{name:PAYMENTS SQUAD}' already exists"
