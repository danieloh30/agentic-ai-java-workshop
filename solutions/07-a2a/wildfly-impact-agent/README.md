# Optional Exercise 7B — WildFly impact agent

Riley's Apex Systems impact-assessment agent, hosted on WildFly with LangChain4j CDI. The Quarkus Exercise 7 application remains the A2A caller. This is an optional 15–20 minute extension.

See the [exercise guide](../../../docs/07-a2a/WILDFLY.md) for the enterprise context, verification steps, and discussion prompts.

Prerequisites: Java 25, the existing workshop Maven wrapper, an exported `OPENAI_API_KEY`, and running Podman or Docker. Free port 8888 by stopping Exercise 7's Quarkus remote agent first.

```bash
CONTAINER_ENGINE=podman ./run.sh up
# Or: CONTAINER_ENGINE=docker ./run.sh up
curl -fsS http://localhost:8888/.well-known/agent-card.json
```

The launcher builds `ROOT.war`, deploys it into `quay.io/wildfly/wildfly:42.0.0.Final-jdk25`, and maps host 8888 to container 8080. The application supplies synchronous A2A discovery and message handling; it does not implement persistent tasks or streaming.

Configuration: `OPENAI_API_KEY` is required; `OPENAI_MODEL` defaults to `gpt-4o`; `OPENAI_BASE_URL` defaults to OpenAI's `/v1` endpoint. The launcher sets `PUBLIC_AGENT_URL` from `WF_PORT` (default 8888). Keep the standard port when using the unchanged Quarkus caller.

Run the offline container/CDI/A2A integration test without an external LLM:

```bash
CONTAINER_ENGINE=podman python3 smoke.py
```

The smoke test uses its own container and port 18889. It also runs the Quarkus caller's `WildFlyInteropTest` on that application's actual dependency graph. GitHub Actions runs the same test with Docker. The Maven unit tests cover SDK serialization, multiline input preservation, input rejection before model calls, and provider-error sanitization. `WildFlyInteropTest` runs against the actual deployed container when `-Dwildfly.url` is supplied by the smoke test.

```bash
CONTAINER_ENGINE=podman ./run.sh logs
CONTAINER_ENGINE=podman ./run.sh down
```
