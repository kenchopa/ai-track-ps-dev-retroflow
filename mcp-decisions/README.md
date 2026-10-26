# retroflow-decisions: our own MCP server

A small local MCP server that makes RetroFlow's `decisions.md` readable for an AI tool. It has the
same two tools as the hosted `ai-track-standards` server:

| Tool | What it does |
|---|---|
| `list_documents` | Lists the documents it serves (today: `decisions`) |
| `read_document` | Returns the current text of one document by id |

It is built with the official MCP SDK for Node.js (`@modelcontextprotocol/sdk` 1.32.1) and the
**stdio** transport: the AI tool starts it as a child process, with one client at a time and no
network. It isn't part of the RetroFlow application, so it lives outside `src/`.

## Run it

```bash
cd mcp-decisions
npm ci        # Node 20+
npm test      # starts the server as an MCP client would, and checks both tools
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
