# retroflow-decisions: our own MCP server

A small local MCP server for RetroFlow. It serves our own `decisions.md`, like the hosted
`ai-track-standards` server serves the standards, and it can also **do** something: check the code.

| Tool | What it does |
|---|---|
| `list_documents` | Lists the documents it serves (today: `decisions`) |
| `read_document` | Returns the current text of one document by id |
| `check_standards` | Runs mechanical StackCraft checks on the code, **without a model** (bonus challenge 1) |

## check_standards

`check_standards(path = "src", rules = all)` checks six rules in plain code:

| Rule | What it checks |
|---|---|
| §1.2 | class files over 200 lines |
| §1.3 | methods over 20 lines, blank lines and comments excluded |
| §1.4 | `@Autowired`/`@Inject` on a field (constructors are fine) |
| §5.1 | hard-coded secrets in `.properties`/`.yml`/`.java` (empty values and `${ENV}` are fine) |
| §5.3 | string concatenation inside `@Query`/`createQuery` |
| §6.1 | public methods of `@Service` classes without JavaDoc, a description, `@param` or `@return` |

- **Every finding** has `file`, `line`, `rule` (e.g. `§1.4`), and the `title` and `severity` from
  the standards document.
- **The severities are not hard-coded.** They're read live from the hosted `ai-track-standards`
  server (`standards.js`), from each section's "Violation severity" line. If the document changes,
  the severities change with it. If a rule disappears from the document, the report warns that its
  findings may be obsolete. If the server can't be reached, the checks still run, with severity
  `unknown` and a warning.
- **Bad input is an error the AI can read, not a crash:** an unknown rule, a path that doesn't
  exist, or a path outside the repository (`../../`, `/etc`, or a symlink that points out).
- **Java is sanitised before checking:** comments and string contents are blanked out and line
  numbers are kept, so a `{` inside a string can't break the brace counting.
- **Known gaps:** `@throws` isn't checked (it needs a compiler to know what a method throws), and
  §6.1 only looks at `@Service` classes, not a `@Component` in the service package. Anything that
  needs judgement (naming, status codes, validation at the boundary) is left to the AI. The report
  says so in `notChecked`.

It is built with the official MCP SDK for Node.js (`@modelcontextprotocol/sdk` 1.32.1) and the
**stdio** transport: the AI tool starts it as a child process, with one client at a time and no
network. It isn't part of the RetroFlow application, so it lives outside `src/`.

## Run it

```bash
cd mcp-decisions
npm ci        # Node 20+
npm test      # 14 tests: every rule on fixtures, bad input, and the server over stdio
```

## Connect your tool

- **Claude Code:** it's already in `.mcp.json` in the root of the repo (`retroflow-decisions`).
  Start `claude` in the repo root and approve the project server once. Or register it yourself
  with `claude mcp add retroflow-decisions -- node mcp-decisions/server.js`.
- **VS Code / Copilot:** add this to `.vscode/mcp.json` and click **Start**:
  ```json
  { "servers": { "retroflow-decisions": { "type": "stdio", "command": "node", "args": ["mcp-decisions/server.js"] } } }
  ```

## Design choices

- **Ids, not paths.** `read_document` only accepts ids from a fixed list. `"../pom.xml"` returns an
  error, so the AI can't use the server to read other files.
- **Read on every call.** When `decisions.md` changes, the next answer uses the new text. Nothing
  needs a restart.
- **stdout is for the protocol.** The only log line goes to stderr. With stdio, anything printed
  to stdout breaks the connection.
- **Dependencies pinned** to exact versions. They were checked on npm first, and `zod` 4.6.7 was
  chosen over 4.6.5 because of a high-severity advisory (GHSA-mrhx-c7xc-m3w5, fixed in 4.6.6).
