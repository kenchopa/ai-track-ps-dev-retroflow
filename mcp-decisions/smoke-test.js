// Starts server.js as a real MCP client would (stdio) and checks both tools.
// Run with: npm test

import assert from "node:assert/strict";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

const client = new Client({ name: "smoke-test", version: "1.0.0" });
await client.connect(
  new StdioClientTransport({ command: process.execPath, args: [new URL("server.js", import.meta.url).pathname] }),
);

const { tools } = await client.listTools();
assert.deepEqual(tools.map((tool) => tool.name).sort(), ["list_documents", "read_document"]);

const list = await client.callTool({ name: "list_documents", arguments: {} });
assert.match(list.content[0].text, /^- decisions: /);

const doc = await client.callTool({ name: "read_document", arguments: { id: "decisions" } });
assert.equal(doc.isError, undefined);
assert.match(doc.content[0].text, /^# RetroFlow: team decisions/);

const unknown = await client.callTool({ name: "read_document", arguments: { id: "../pom.xml" } });
assert.equal(unknown.isError, true);
assert.match(unknown.content[0].text, /Unknown document/);

await client.close();
console.log("smoke test passed: 2 tools, list, read, unknown id refused");
