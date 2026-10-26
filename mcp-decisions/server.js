#!/usr/bin/env node
// Local MCP server (stdio) for RetroFlow. It serves our own decisions (list_documents,
// read_document, like the hosted ai-track-standards server) and it can *do* something:
// check_standards runs mechanical StackCraft checks on the code, without a model.
// stdout carries the MCP protocol, so anything else goes to stderr.

import { readFile, realpath, stat } from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";
import { RULE_IDS, runChecks } from "./checks.js";
import { loadStandards } from "./standards.js";

// Only these files can be read: the id is a key, never a path.
const DOCUMENTS = {
  decisions: {
    title: "RetroFlow: team decisions (our interpretation of the brief)",
    file: new URL("../decisions.md", import.meta.url),
  },
};

const server = new McpServer({ name: "retroflow-decisions", version: "1.0.0" });

server.registerTool(
  "list_documents",
  {
    title: "List documents",
    description:
      "List the RetroFlow team documents this server can read: id and title. " +
      "Call this first if you are not sure which id to use.",
  },
  async () => ({
    content: [
      {
        type: "text",
        text: Object.entries(DOCUMENTS)
          .map(([id, doc]) => `- ${id}: ${doc.title}`)
          .join("\n"),
      },
    ],
  }),
);

server.registerTool(
  "read_document",
  {
    title: "Read document",
    description:
      "Read the full, current text of one RetroFlow team document by id (e.g. \"decisions\"). " +
      "Use it for questions about what the team decided: status codes, validation rules, " +
      "the entity model, the endpoint contract and open questions.",
    inputSchema: { id: z.string().describe('Document id, e.g. "decisions". See list_documents.') },
  },
  async ({ id }) => {
    const doc = Object.hasOwn(DOCUMENTS, id) ? DOCUMENTS[id] : undefined;
    if (!doc) {
      return {
        isError: true,
        content: [{ type: "text", text: `Unknown document "${id}". Valid ids: ${Object.keys(DOCUMENTS).join(", ")}.` }],
      };
    }
    // Read on every call, so the AI always sees the file as it is now.
    const text = await readFile(doc.file, "utf8");
    return { content: [{ type: "text", text }] };
  },
);

// The only folder check_standards may read: the RetroFlow repository this server lives in.
const PROJECT_ROOT = await realpath(fileURLToPath(new URL("..", import.meta.url)));
const SEVERITY_ORDER = ["Critical", "Major", "Minor"];

server.registerTool(
  "check_standards",
  {
    title: "Check StackCraft standards",
    description:
      "Run mechanical checks of StackCraft's coding standards on RetroFlow's source code, without a model: " +
      `class length (§1.2), method length (§1.3), field injection (§1.4), hard-coded secrets (§5.1), ` +
      `query concatenation (§5.3) and JavaDoc on service methods (§6.1). Returns findings with file, line, ` +
      "rule and the severity read from the standards document. Use it first when asked to audit or check " +
      "compliance, then review what it can't check (naming, status codes, design) yourself.",
    inputSchema: {
      path: z
        .string()
        .default("src")
        .describe('File or folder relative to the repository root, e.g. "src", "src/main" or "src/main/resources"'),
      rules: z
        .array(z.string())
        .optional()
        .describe(`Rule ids to run, e.g. ["1.4", "§5.1"]. Default: all of ${RULE_IDS.join(", ")}`),
    },
  },
  async ({ path: requested, rules }) => {
    const ruleIds = (rules ?? RULE_IDS).map((id) => id.replace(/^§/, "").trim());
    const unknown = ruleIds.filter((id) => !RULE_IDS.includes(id));
    if (unknown.length) {
      return error(`Unknown rule ${unknown.map((id) => `"${id}"`).join(", ")}. Rules this tool checks: ${RULE_IDS.join(", ")}.`);
    }
    const target = await resolveInsideProject(requested);
    if (target.error) return error(target.error);

    const { filesScanned, findings } = await runChecks(PROJECT_ROOT, target.relative, ruleIds);
    const standards = await loadStandards().catch(() => null);
    const warnings = [];
    if (!standards) warnings.push("The standards server could not be reached: severities and titles are unknown in this run.");
    for (const id of ruleIds) {
      if (standards && !standards.sections[id]) warnings.push(`§${id} is no longer in the standards document (v${standards.version}): its findings may be obsolete.`);
    }
    const enriched = findings
      .map((f) => ({
        file: f.file,
        line: f.line,
        rule: `§${f.rule}`,
        title: standards?.sections[f.rule]?.title ?? "unknown",
        severity: standards?.sections[f.rule]?.severity ?? "unknown",
        message: f.message,
      }))
      .sort((a, b) => rank(a.severity) - rank(b.severity) || a.file.localeCompare(b.file) || a.line - b.line || a.rule.localeCompare(b.rule));
    const counts = Object.fromEntries(SEVERITY_ORDER.map((s) => [s, enriched.filter((f) => f.severity === s).length]));
    const report = {
      standards: standards ? `stackcraft-coding v${standards.version}, read from the ai-track-standards server` : "unavailable",
      path: target.relative,
      rules: ruleIds.map((id) => `§${id}`),
      filesScanned,
      counts,
      warnings,
      findings: enriched,
      notChecked: "Anything that needs judgement: naming, status codes, validation at the boundary, test quality. Review those yourself.",
    };
    return { content: [{ type: "text", text: JSON.stringify(report, null, 2) }] };
  },
);

/** Resolves a user-supplied path and refuses anything outside the repository, symlinks included. */
async function resolveInsideProject(requested) {
  if (requested.includes("\0")) return { error: "The path contains a null byte." };
  const candidate = path.resolve(PROJECT_ROOT, requested);
  let real;
  try {
    real = await realpath(candidate);
    await stat(real);
  } catch {
    return { error: `Path "${requested}" does not exist in the RetroFlow repository.` };
  }
  if (real !== PROJECT_ROOT && !real.startsWith(PROJECT_ROOT + path.sep)) {
    return { error: `Path "${requested}" is outside the RetroFlow repository. check_standards only reads files inside it.` };
  }
  return { relative: path.relative(PROJECT_ROOT, real) || "." };
}

function rank(severity) {
  const index = SEVERITY_ORDER.indexOf(severity);
  return index === -1 ? SEVERITY_ORDER.length : index;
}

function error(text) {
  return { isError: true, content: [{ type: "text", text }] };
}

await server.connect(new StdioServerTransport());
console.error("retroflow-decisions MCP server running on stdio (project root: " + PROJECT_ROOT + ")");
