#!/usr/bin/env node
// Local MCP server (stdio) that serves RetroFlow's own decisions to an AI tool.
// Same two tools as the hosted ai-track-standards server: list_documents and read_document.
// stdout carries the MCP protocol, so anything else goes to stderr.

import { readFile } from "node:fs/promises";
import { fileURLToPath } from "node:url";
import { McpServer } from "@modelcontextprotocol/sdk/server/mcp.js";
import { StdioServerTransport } from "@modelcontextprotocol/sdk/server/stdio.js";
import { z } from "zod";

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

await server.connect(new StdioServerTransport());
console.error("retroflow-decisions MCP server running on stdio");
