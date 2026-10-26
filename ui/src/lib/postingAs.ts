import { useCallback, useEffect, useState } from "react";

const key = (teamId: number) => `retroflow.postingAs.${teamId}`;

function read(teamId: number): string | null {
  try {
    return window.localStorage.getItem(key(teamId));
  } catch {
    return null;
  }
}

/**
 * Who is adding feedback. The API has no login (decision G6) but needs an author who is a team
 * member, so the UI asks once per team and remembers the answer in this browser.
 */
export function usePostingAs(teamId: number, members: string[]): [string, (member: string) => void] {
  const pick = useCallback(() => {
    const stored = read(teamId);
    return stored && members.includes(stored) ? stored : (members[0] ?? "");
  }, [teamId, members]);
  const [member, setMember] = useState(pick);

  useEffect(() => setMember(pick()), [pick]);

  const choose = useCallback(
    (next: string) => {
      setMember(next);
      try {
        window.localStorage.setItem(key(teamId), next);
      } catch {
        // Private mode or blocked storage: the choice still holds for this session.
      }
    },
    [teamId],
  );
  return [member, choose];
}
