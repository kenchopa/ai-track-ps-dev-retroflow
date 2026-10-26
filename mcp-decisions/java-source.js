// Just enough Java parsing for mechanical checks. No model, no compiler:
// blank out comments and the contents of string literals (keeping line numbers),
// so a "{" inside a string can't throw off the brace counting (the Block 3
// false positive in TeamControllerTest:62).

/** Returns the source with comments removed and string/char contents replaced by "_". */
export function sanitize(source) {
  let out = "";
  let i = 0;
  const n = source.length;
  while (i < n) {
    const c = source[i];
    const next = source[i + 1];
    if (c === "/" && next === "/") {
      while (i < n && source[i] !== "\n") i++;
    } else if (c === "/" && next === "*") {
      i += 2;
      while (i < n && !(source[i] === "*" && source[i + 1] === "/")) {
        out += source[i] === "\n" ? "\n" : "";
        i++;
      }
      i += 2;
    } else if (source.startsWith('"""', i)) {
      out += '"""';
      i += 3;
      while (i < n && !source.startsWith('"""', i)) {
        out += source[i] === "\n" ? "\n" : "_";
        i += source[i] === "\\" ? 2 : 1;
      }
      out += '"""';
      i += 3;
    } else if (c === '"' || c === "'") {
      out += c;
      i++;
      while (i < n && source[i] !== c && source[i] !== "\n") {
        out += "_";
        i += source[i] === "\\" ? 2 : 1;
      }
      out += c;
      i++;
    } else {
      out += c;
      i++;
    }
  }
  return out;
}

const NOT_A_METHOD = new Set(["if", "for", "while", "switch", "catch", "return", "new", "else", "try", "synchronized", "do"]);
const HEADER = /^\s*(?:@\w+(?:\([^)]*\))?\s+)*((?:(?:public|protected|private|static|final|abstract|default|synchronized)\s+)*)(?:<[^>]+>\s+)?(?:([\w<>\[\],.? ]+?)\s+)?(\w+)\s*\(/;

/**
 * Methods and constructors with their header line, body lines and modifiers.
 * Lines are 1-based. bodyLines counts non-blank lines between the braces.
 */
export function methods(sanitized) {
  const lines = sanitized.split("\n");
  const found = [];
  let depth = 0;
  for (let i = 0; i < lines.length; i++) {
    const match = depth >= 1 ? HEADER.exec(lines[i]) : null;
    if (match && !NOT_A_METHOD.has(match[3]) && !/\b(class|record|interface|enum)\b/.test(lines[i])) {
      const method = readMethod(lines, i, match);
      if (method) {
        found.push(method);
        i = method.endIndex;
        continue;
      }
    }
    depth += count(lines[i], "{") - count(lines[i], "}");
  }
  return found;

  function readMethod(all, start, match) {
    let j = start;
    while (j < all.length && !all[j].includes("{")) {
      if (all[j].includes(";")) return null; // abstract or interface method, or a call
      j++;
    }
    if (j >= all.length) return null;
    let level = 0;
    let body = 0;
    for (let k = j; k < all.length; k++) {
      level += count(all[k], "{") - count(all[k], "}");
      if (k > j && level > 0 && all[k].trim() !== "") body++;
      if (level === 0) {
        return {
          name: match[3],
          modifiers: match[1].trim().split(/\s+/).filter(Boolean),
          returnType: match[2] ?? null,
          line: start + 1,
          bodyLines: body,
          headerText: all.slice(start, j + 1).join(" "),
          endIndex: k,
        };
      }
    }
    return null;
  }
}

function count(text, ch) {
  return text.split(ch).length - 1;
}
