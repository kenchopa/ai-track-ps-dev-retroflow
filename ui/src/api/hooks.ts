import { useMutation, useQueries, useQuery, useQueryClient } from "@tanstack/react-query";
import { api } from "./client";
import type { ActionItem, ActionItemFilter, FeedbackItem, Priority, Retrospective } from "./types";

export const keys = {
  teams: ["teams"] as const,
  team: (id: number) => ["team", id] as const,
  retros: (teamId: number) => ["retros", teamId] as const,
  retro: (id: number) => ["retro", id] as const,
  items: (retroId: number) => ["items", retroId] as const,
  teamActions: (teamId: number) => ["team-actions", teamId] as const,
};

export function useTeams() {
  return useQuery({ queryKey: keys.teams, queryFn: api.teams });
}

export function useTeam(id: number, enabled = true) {
  return useQuery({ queryKey: keys.team(id), queryFn: () => api.team(id), enabled });
}

export function useRetrospectives(teamId: number) {
  return useQuery({ queryKey: keys.retros(teamId), queryFn: () => api.retrospectives(teamId) });
}

/** The OPEN retrospective of every team, for the navigation and the overview. */
export function useOpenRetros(teamIds: number[]) {
  const results = useQueries({
    queries: teamIds.map((teamId) => ({ queryKey: keys.retros(teamId), queryFn: () => api.retrospectives(teamId) })),
  });
  const open = new Map<number, Retrospective>();
  results.forEach((result, index) => {
    const retro = result.data?.find((candidate) => candidate.status === "OPEN");
    const teamId = teamIds[index];
    if (retro && teamId !== undefined) open.set(teamId, retro);
  });
  return open;
}

export function useRetrospective(id: number) {
  return useQuery({ queryKey: keys.retro(id), queryFn: () => api.retrospective(id) });
}

/** While a retro is open the whole team adds items at once, so the board refreshes every 5 s. */
export function useFeedbackItems(retroId: number, live = false) {
  return useQuery({
    queryKey: keys.items(retroId),
    queryFn: () => api.feedbackItems(retroId),
    refetchInterval: live ? 5_000 : false,
  });
}

export function useTeamActionItems(teamId: number, filter: ActionItemFilter) {
  return useQuery({
    queryKey: [...keys.teamActions(teamId), filter],
    queryFn: () => api.teamActionItems(teamId, filter),
  });
}

export function useCreateTeam() {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ name, members }: { name: string; members: string[] }) => api.createTeam(name, members),
    onSuccess: () => client.invalidateQueries({ queryKey: keys.teams }),
  });
}

export function useCreateRetrospective(teamId: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: ({ title, date }: { title: string; date?: string }) => api.createRetrospective(teamId, title, date),
    onSuccess: () => client.invalidateQueries({ queryKey: keys.retros(teamId) }),
  });
}

export function useCloseRetrospective(retro: Retrospective) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: () => api.closeRetrospective(retro.id),
    onSuccess: (closed) => {
      client.setQueryData(keys.retro(retro.id), closed);
      return client.invalidateQueries({ queryKey: keys.retros(retro.teamId) });
    },
  });
}

/** Everything that changes an item: refresh the board and the team's action item list. */
function useItemMutation<TVariables, TResult>(retro: Retrospective, mutationFn: (variables: TVariables) => Promise<TResult>) {
  const client = useQueryClient();
  return useMutation({
    mutationFn,
    onSettled: () =>
      Promise.all([
        client.invalidateQueries({ queryKey: keys.items(retro.id) }),
        client.invalidateQueries({ queryKey: keys.teamActions(retro.teamId) }),
      ]),
  });
}

export function useAddFeedback(retro: Retrospective) {
  return useItemMutation(retro, (input: { content: string; type: "WENT_WELL" | "NEEDS_IMPROVEMENT"; author: string }) =>
    api.addFeedbackItem(retro.id, input.content, input.type, input.author),
  );
}

export function useAddActionItem(retro: Retrospective) {
  return useItemMutation(retro, (input: { content: string; priority: Priority; author: string }) =>
    api.addActionItem(retro.id, input.content, input.priority, input.author),
  );
}

export function useUpdateItem(retro: Retrospective) {
  return useItemMutation(retro, (input: { item: FeedbackItem; content: string; priority?: Priority }) =>
    input.item.type === "ACTION_ITEM"
      ? api.updateActionItem(input.item.id, input.content, input.priority ?? input.item.priority ?? "MEDIUM")
      : api.updateFeedbackItem(input.item.id, input.content),
  );
}

export function useDeleteItem(retro: Retrospective) {
  return useItemMutation(retro, (item: FeedbackItem) => api.deleteFeedbackItem(item.id));
}

/** Completing is one-way, so the board shows it at once and rolls back only if the API says no. */
export function useCompleteActionItem(teamId: number, retroId?: number) {
  const client = useQueryClient();
  return useMutation({
    mutationFn: (item: ActionItem) => api.completeActionItem(item.id),
    onMutate: async (item) => {
      if (retroId === undefined) return undefined;
      await client.cancelQueries({ queryKey: keys.items(retroId) });
      const previous = client.getQueryData<FeedbackItem[]>(keys.items(retroId));
      client.setQueryData<FeedbackItem[]>(keys.items(retroId), (items) =>
        items?.map((candidate) => (candidate.id === item.id ? { ...candidate, completed: true } : candidate)),
      );
      return previous;
    },
    onError: (_error, _item, previous) => {
      if (retroId !== undefined && previous) client.setQueryData(keys.items(retroId), previous);
    },
    onSettled: () =>
      Promise.all([
        retroId === undefined ? undefined : client.invalidateQueries({ queryKey: keys.items(retroId) }),
        client.invalidateQueries({ queryKey: keys.teamActions(teamId) }),
      ]),
  });
}

export function useSummary(retroId: number) {
  return useMutation({ mutationFn: () => api.summarise(retroId) });
}

