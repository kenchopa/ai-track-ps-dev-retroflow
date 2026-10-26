// Talks to server.js as a real MCP client would (stdio).

import assert from "node:assert/strict";
import { symlink, unlink } from "node:fs/promises";
import { after, before, test } from "node:test";
import { fileURLToPath } from "node:url";
import { Client } from "@modelcontextprotocol/sdk/client/index.js";
import { StdioClientTransport } from "@modelcontextprotocol/sdk/client/stdio.js";

const LINK = fileURLToPath(new URL("fixtures/link-outside", import.meta.url));
let client;

before(async () => {
  client = new Client({ name: "server-test", version: "1.0.0" });
  await client.connect(
    new StdioClientTransport({ command: process.execPath, args: [fileURLToPath(new URL("../server.js", import.meta.url))], stderr: "ignore" }),
  );
});

after(async () => {
  await unlink(LINK).catch(() => {});
  await client.close();
});

const call = (name, args = {}) => client.callTool({ name, arguments: args });
const text = (result) => result.content[0].text;

test("offers the three tools", async () => {
  const { tools } = await client.listTools();
  assert.deepEqual(tools.map((t) => t.name).sort(), ["check_standards", "list_documents", "read_document"]);
});

test("list_documents and read_document serve decisions.md, and refuse a path as id", async () => {
  assert.match(text(await call("list_documents")), /^- decisions: /);
  assert.match(text(await call("read_document", { id: "decisions" })), /^# RetroFlow: team decisions/);
  const refused = await call("read_document", { id: "../pom.xml" });
  assert.equal(refused.isError, true);
  assert.match(text(refused), /Unknown document/);
});

test("check_standards returns file, line, rule and severity", async () => {
  const report = JSON.parse(text(await call("check_standards", { path: "mcp-decisions/test/fixtures/src", rules: ["§1.4"] })));
  assert.equal(report.findings.length, 1);
  const [finding] = report.findings;
  assert.equal(finding.file, "mcp-decisions/test/fixtures/src/main/BadService.java");
  assert.equal(finding.line, 10);
  assert.equal(finding.rule, "§1.4");
  assert.ok(["Critical", "unknown"].includes(finding.severity), finding.severity);
});

test("check_standards gives the same answer twice", async () => {
  const args = { path: "src" };
  assert.equal(text(await call("check_standards", args)), text(await call("check_standards", args)));
});

test("bad input is a clear error, not a crash", async () => {
  const cases = [
    [{ rules: ["9.9"] }, /Unknown rule "9.9"/],
    [{ path: "does/not/exist" }, /does not exist/],
    [{ path: "../../" }, /outside the RetroFlow repository/],
    [{ path: "/etc" }, /outside the RetroFlow repository/],
  ];
  for (const [args, message] of cases) {
    const result = await call("check_standards", args);
    assert.equal(result.isError, true, JSON.stringify(args));
    assert.match(text(result), message);
  }
});

test("a symlink inside the repo that points outside is refused", async () => {
  await symlink("/etc", LINK);
  const result = await call("check_standards", { path: "mcp-decisions/test/fixtures/link-outside" });
  assert.equal(result.isError, true);
  assert.match(text(result), /outside the RetroFlow repository/);
});
