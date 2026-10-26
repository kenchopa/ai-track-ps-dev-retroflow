// Reads the rule titles and severities from the hosted StackCraft standards
// server, so they aren't hard-coded here. Only the *checks* live in our code.

import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StreamableHTTPClientTransport } from "@modelcontextprotocol/sdk/client/streamableHttp.js";

const STANDARDS_URL = process.env.STANDARDS_MCP_URL ?? "https://ai-track-ps-production.up.railway.app/mcp";
const DOCUMENT_ID = "stackcraft-coding";
const TIMEOUT_MS = 10_000;

let cached;

/**
 * { version, sections: { "1.4": { title, severity } } }, read once per process.
 * Throws when the server can't be reached; the caller decides what to do then.
 */
export async function loadStandards() {
  cached ??= fetchStandards().catch((error) => {
    cached = undefined; // retry on the next call
    throw error;
  });
  return cached;
}

/** Parses "### 1.4 Title (CRITICAL)" headings and their "Violation severity: **X**" lines. */
export function parseStandards(markdown) {
  const version = /\*\*Version ([\d.]+)/.exec(markdown)?.[1] ?? "unknown";
  const sections = {};
  for (const block of markdown.split(/^### /m).slice(1)) {
    const heading = /^(\d+\.\d+)\s+(.+?)\s*$/m.exec(block);
    if (!heading) continue;
    const title = heading[2].replace(/\s*\(CRITICAL\)$/i, "");
    const severity = /Violation severity:\s*\*\*(\w+)\*\*/.exec(block)?.[1] ?? "not stated";
    sections[heading[1]] = { title, severity };
  }
  return { version, sections };
}

async function fetchStandards() {
  const client = new Client({ name: "retroflow-check-standards", version: "1.0.0" });
  const signal = AbortSignal.timeout(TIMEOUT_MS);
  try {
    await client.connect(new StreamableHTTPClientTransport(new URL(STANDARDS_URL)), { signal });
    const result = await client.callTool({ name: "read_document", arguments: { id: DOCUMENT_ID } }, undefined, { signal });
    if (result.isError) throw new Error(`standards server refused read_document("${DOCUMENT_ID}")`);
    return parseStandards(result.content.map((part) => part.text ?? "").join("\n"));
  } finally {
    await client.close().catch(() => {});
  }
}
