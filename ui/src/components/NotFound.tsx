import { Compass } from "@phosphor-icons/react";
import { Link, useRouteError } from "react-router";
import { messageOf } from "../api/errors";
import { Page } from "./Page";
import { EmptyState } from "./ui/Feedback";

export function NotFound({ message }: { message?: string }) {
  return (
    <Page>
      <EmptyState
        icon={<Compass />}
        title="This page doesn't exist"
        description={message ?? "The link may be old, or the team or retrospective was never created."}
        action={
          <Link to="/" className="focus-ring rounded-md text-sm font-medium text-accent hover:text-accent-strong">
            Go to all teams
          </Link>
        }
      />
    </Page>
  );
}

/** Last resort for an error while rendering a route. */
export function RouteError() {
  const error = useRouteError();
  return (
    <div className="min-h-[100dvh] bg-canvas">
      <NotFound message={`Something went wrong: ${messageOf(error)}`} />
    </div>
  );
}
