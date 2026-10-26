// Mechanical StackCraft checks: plain code, no model. Same input, same output.
// Each rule returns findings { file, line, rule, message }; severity and title
// are added from the standards document by the caller.

import { readdir, readFile } from "node:fs/promises";
import path from "node:path";
import { methods, sanitize } from "./java-source.js";

const SECRET_NAME = /(password|passwd|secret|token|api[-_.]?key)/i;

export const RULES = {
  "1.2": {
    files: /\.java$/,
    check({ lines }) {
      return lines.length > 200 ? [{ line: 1, message: `class file is ${lines.length} lines (max 200)` }] : [];
    },
  },
  "1.3": {
    files: /\.java$/,
    check({ sanitized }) {
      return methods(sanitized)
        .filter((m) => m.bodyLines > 20)
        .map((m) => ({ line: m.line, message: `${m.name}() has ${m.bodyLines} lines (max 20, blank lines and comments excluded)` }));
    },
  },
  "1.4": {
    files: /\.java$/,
    check({ sanitized }) {
      const lines = sanitized.split("\n");
      const findings = [];
      lines.forEach((text, i) => {
        if (!/@(Autowired|Inject)\b/.test(text)) return;
        // The declaration the annotation belongs to: same line, or the next line that isn't an annotation.
        let j = i;
        let declaration = text.replace(/^.*@(Autowired|Inject)\b(\([^)]*\))?/, "");
        while (declaration.trim() === "" || /^\s*@/.test(declaration)) {
          j++;
          if (j >= lines.length) return;
          declaration = lines[j];
        }
        if (/;/.test(declaration) && !/\(/.test(declaration)) {
          findings.push({ line: i + 1, message: `field injection: ${declaration.trim()}` });
        }
      });
      return findings;
    },
  },
  "5.1": {
    files: /\.(java|properties|ya?ml)$/,
    check({ file, raw }) {
      const findings = [];
      raw.split("\n").forEach((text, i) => {
        const property = /^\s*([\w.\-\[\]]+)\s*[=:]\s*(.*?)\s*$/.exec(text);
        if (!file.endsWith(".java") && property && SECRET_NAME.test(property[1])) {
          const value = property[2].replace(/^["']|["']$/g, "");
          if (value !== "" && !/^\$\{[^}]*\}$/.test(value)) {
            findings.push({ line: i + 1, message: `hard-coded value for '${property[1]}'` });
          }
        }
        const field = /\b(\w*(?:password|passwd|secret|token|apiKey|api_key)\w*)\s*=\s*"([^"]+)"/i.exec(text);
        if (file.endsWith(".java") && field) {
          findings.push({ line: i + 1, message: `hard-coded string assigned to '${field[1]}'` });
        }
      });
      return findings;
    },
  },
  "5.3": {
    files: /\.java$/,
    check({ sanitized }) {
      const findings = [];
      const queryStart = /@Query\s*\(|\.create(Native)?Query\s*\(/g;
      for (const match of sanitized.matchAll(queryStart)) {
        const argument = sanitized.slice(match.index, closingParen(sanitized, match.index + match[0].length));
        if (/"\s*\+|\+\s*"/.test(argument)) {
          findings.push({ line: lineOf(sanitized, match.index), message: "string concatenation in a query" });
        }
      }
      return findings;
    },
  },
  "6.1": {
    files: /\.java$/,
    check({ raw, sanitized }) {
      if (!/@Service\b/.test(sanitized)) return [];
      const rawLines = raw.split("\n");
      return methods(sanitized)
        .filter((m) => m.modifiers.includes("public") && m.returnType !== null)
        .flatMap((m) => {
          const missing = javadocGaps(rawLines, m);
          return missing.length ? [{ line: m.line, message: `${m.name}(): JavaDoc ${missing.join(", ")}` }] : [];
        });
    },
  },
};

export const RULE_IDS = Object.keys(RULES);

/** Runs the given rules over every matching file under root/relativePath. */
export async function runChecks(root, relativePath, ruleIds) {
  const start = path.join(root, relativePath);
  const files = (await listFiles(start)).sort();
  const findings = [];
  for (const absolute of files) {
    const file = path.relative(root, absolute).split(path.sep).join("/");
    const applicable = ruleIds.filter((id) => RULES[id].files.test(file));
    if (applicable.length === 0) continue;
    const raw = await readFile(absolute, "utf8");
    const source = { file, raw, lines: raw.replace(/\n$/, "").split("\n"), sanitized: file.endsWith(".java") ? sanitize(raw) : raw };
    for (const id of applicable) {
      for (const finding of RULES[id].check(source)) findings.push({ file, rule: id, ...finding });
    }
  }
  return { filesScanned: files.length, findings };
}

async function listFiles(start) {
  const entries = await readdir(start, { withFileTypes: true, recursive: true }).catch(async (error) => {
    if (error.code === "ENOTDIR") return null;
    throw error;
  });
  if (entries === null) return [start];
  return entries
    .filter((e) => e.isFile() && !/(^|[\\/])(node_modules|target|\.git)([\\/]|$)/.test(path.join(e.parentPath, e.name)))
    .map((e) => path.join(e.parentPath, e.name));
}

function javadocGaps(rawLines, method) {
  let i = method.line - 2;
  while (i >= 0 && /^\s*@/.test(rawLines[i])) i--; // skip annotations
  if (i < 0 || !/\*\/\s*$/.test(rawLines[i])) return ["missing"];
  let start = i;
  while (start >= 0 && !/\/\*\*/.test(rawLines[start])) start--;
  const doc = rawLines.slice(start, i + 1).join("\n");
  const gaps = [];
  const description = doc.replace(/\/\*\*|\*\/|^\s*\*/gm, "").split(/\n\s*@/)[0].trim();
  if (!description) gaps.push("no description");
  const params = /\(([^)]*)\)/.exec(method.headerText)?.[1].split(",").map((p) => p.trim().split(/\s+/).pop()).filter(Boolean) ?? [];
  for (const name of params) if (!new RegExp(`@param\\s+${name}\\b`).test(doc)) gaps.push(`no @param ${name}`);
  if (method.returnType && method.returnType.trim() !== "void" && !/@return\b/.test(doc)) gaps.push("no @return");
  return gaps;
}

function closingParen(text, from) {
  let level = 1;
  for (let i = from; i < text.length; i++) {
    if (text[i] === "(") level++;
    if (text[i] === ")" && --level === 0) return i;
  }
  return text.length;
}

function lineOf(text, index) {
  return text.slice(0, index).split("\n").length;
}
