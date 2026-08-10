# Exercise 05 — MCP Client + OpenCode CLI AI Governance

This exercise has two linked parts:

**Part A — MCP Client (`@McpToolBox`)**

- `weather-mcp-server/` — a Quarkus MCP server running on port **8081**  
  Exposes one tool: `getForecast(latitude, longitude)` via SSE transport.
- This directory — the MCP client app running on port **8080**  
  `CustomerSupportAgent` (`@RegisterAiService`, `@SessionScoped`) mixes:
  - `@ToolBox(BookingRepository.class)` — local JPA tools (list, get, cancel bookings)
  - `@McpToolBox("weather")` — remote MCP tool (weather forecast)

**Part B — OpenCode CLI AI Governance**

- `lab/AGENTS.md` — project governance file authored and validated by OpenCode CLI
- Demonstrates: token savings (163 vs ~800 tokens), approval-gate, guardrail refusal

---

## Quick start

```bash
# Terminal 1: MCP server (port 8081)
cd solutions/05-ai-governance/weather-mcp-server
./mvnw quarkus:dev

# Terminal 2: MCP client app (port 8080)
cd solutions/05-ai-governance
./mvnw quarkus:dev
```

Open `http://localhost:8080` and chat with "Miles of Smiles" customer support.

---

## Lab guide

**Docs:** [`docs/05-ai-governance/START_HERE.md`](../../docs/05-ai-governance/START_HERE.md)
