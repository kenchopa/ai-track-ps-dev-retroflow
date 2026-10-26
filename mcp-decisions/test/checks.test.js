import assert from "node:assert/strict";
import { mkdir, mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import path from "node:path";
import { test } from "node:test";
import { fileURLToPath } from "node:url";
import { RULES, runChecks } from "../checks.js";
import { parseStandards } from "../standards.js";

const FIXTURES = fileURLToPath(new URL("fixtures", import.meta.url));
const BAD = "src/main/BadService.java";
const GOOD = "src/main/GoodService.java";
const PROPERTIES = "src/main/resources/application.properties";

async function findings(rule) {
  const { findings: all } = await runChecks(FIXTURES, "src", [rule]);
  return all.map(({ file, line, rule: id }) => `${file}:${line} §${id}`);
}

test("§1.3 counts body lines without blanks or comments, and ignores braces inside strings", async () => {
  assert.deepEqual(await findings("1.3"), [`${BAD}:31 §1.3`]);
  const { findings: [finding] } = await runChecks(FIXTURES, BAD, ["1.3"]);
  assert.match(finding.message, /tooLong\(\) has 22 lines/);
});

test("§1.4 flags @Autowired on a field, not on a constructor", async () => {
  assert.deepEqual(await findings("1.4"), [`${BAD}:10 §1.4`]);
});

test("§5.1 flags hard-coded secrets but not empty values or ${ENV} placeholders", async () => {
  // The fake secrets are written at runtime, so not even a fake one ends up in git (§5.1 covers history).
  const root = await mkdtemp(path.join(tmpdir(), "check-standards-"));
  const fake = "x".repeat(12);
  await mkdir(path.join(root, "src/main/resources"), { recursive: true });
  await writeFile(path.join(root, "src/main/Keys.java"), `class Keys {\n    private String apiKey = "${fake}";\n}\n`);
  await writeFile(path.join(root, PROPERTIES), `${await readFile(path.join(FIXTURES, PROPERTIES), "utf8")}mail.password=${fake}\n`);
  try {
    const { findings: all } = await runChecks(root, "src", ["5.1"]);
    assert.deepEqual(all.map((f) => `${f.file}:${f.line}`), ["src/main/Keys.java:2", `${PROPERTIES}:4`]);
    assert.deepEqual(await findings("5.1"), [], "the committed fixtures contain no secret");
  } finally {
    await rm(root, { recursive: true, force: true });
  }
});

test("§5.3 flags concatenation in a query, not named parameters", async () => {
  assert.deepEqual(await findings("5.3"), [`${BAD}:23 §5.3`]);
});

test("§6.1 flags public service methods without JavaDoc or without a description", async () => {
  const { findings: all } = await runChecks(FIXTURES, "src", ["6.1"]);
  assert.deepEqual(
    all.map((f) => `${f.file}:${f.line} ${f.message}`),
    [`${BAD}:15 undocumented(): JavaDoc missing`, `${BAD}:22 noDescription(): JavaDoc no description, no @return`],
  );
  assert.ok(!all.some((f) => f.file === GOOD));
});

test("§1.2 flags a class file over 200 lines", () => {
  const lines = Array.from({ length: 201 }, () => "x");
  assert.equal(RULES["1.2"].check({ lines }).length, 1);
  assert.equal(RULES["1.2"].check({ lines: lines.slice(1) }).length, 0);
});

test("the same input gives the same output", async () => {
  const first = await runChecks(FIXTURES, "src", Object.keys(RULES));
  const second = await runChecks(FIXTURES, "src", Object.keys(RULES));
  assert.deepEqual(first, second);
});

test("severities come from the 'Violation severity' line, not from the heading", () => {
  const { version, sections } = parseStandards(
    "**Version 3.1 ·\n### 1.1 Package structure (CRITICAL)\ntext\nViolation severity: **Major**. Old projects...\n### 3.1 Minimum coverage\nno severity here\n",
  );
  assert.equal(version, "3.1");
  assert.deepEqual(sections["1.1"], { title: "Package structure", severity: "Major" });
  assert.deepEqual(sections["3.1"], { title: "Minimum coverage", severity: "not stated" });
});
