![Workshop cover](docs/images/cover_page.png)

[![Deploy docs to GitHub Pages](https://github.com/danieloh30/agentic-ai-java-workshop/workflows/Deploy%20docs%20to%20GitHub%20Pages/badge.svg)](https://github.com/danieloh30/agentic-ai-java-workshop/actions/workflows/docs.yml)

# Enterprise Agentic AI: Architecting Autonomous Java Systems for Production

**Hands-On Workshop**  
**Duration:** 90 minutes (10 min intro · 80 min hands-on)  
**Lab site:** https://danieloh30.github.io/agentic-ai-java-workshop/ (short URL: [bit.ly/agents-labs](https://bit.ly/agents-labs))  
**Intro deck:** [intro-deck.pdf](docs/images/intro-deck.pdf)

Build intelligent, secure, and observable multi-agent applications on **Quarkus** with **Quarkus LangChain4j** and **OpenCode CLI** — from a single agent to a full supervisor orchestration with human-in-the-loop, OpenTelemetry tracing, and A2A remote agents.

## Repository layout

| Path | Purpose |
|------|---------|
| [`lab/`](lab/) | Your hands-on Quarkus project (stub files with `// TODO`) |
| [`docs/`](docs/) | All lab instructions (Markdown) and images |
| [`solutions/`](solutions/) | Reference solution projects for each exercise |

Start here:

1. **[docs/index.md](docs/index.md)** — lab landing page with exercise table
2. **[solutions/README.md](solutions/README.md)** — exercise → solution mapping

## Quick start

```bash
git clone https://github.com/danieloh30/agentic-ai-java-workshop.git
cd agentic-ai-java-workshop
export OPENAI_API_KEY=sk-your-key-here

# Start your working project for Exercise 1
cd lab
./mvnw quarkus:dev
```

Open http://localhost:8080

## Reset labs

To revert all working code back to the original TODO stubs:

```bash
./reset-labs.sh          # Reset both lab/ (Ex 1-4) and Exercise 08
./reset-labs.sh lab      # Reset root lab only (Exercises 1-4)
./reset-labs.sh ex08     # Reset Exercise 08 lab only
```

## Prerequisites

- **Java 25+**
- Maven 3.9+ (or use `./mvnw` in each exercise)
- Quarkus — kept current via Dependabot
- [OpenCode CLI](https://opencode.ai/) or any AGENTS.md-compatible AI coding assistant
- LLM API key (OpenAI, Anthropic, or compatible provider)
- Free ports **8080**, **8888**
- Docker or Podman (for Quarkus Dev Services)
