# Optional Exercise 7B — The impact agent on WildFly

<span class="badge badge--run-read">Run + Read</span>

**Timebox:** 15–20 additional minutes  
**Persona:** Riley — Apex Systems' SRE team lead  
**Prerequisite:** Complete Exercise 7 first. This extension is optional and does not change the core workshop timebox.

## Enterprise context

Riley's team owns the same business-impact capability introduced in Exercise 7. A collaborating Apex team operates Jakarta EE services on WildFly and wants to host that capability in its existing environment. The incident-management application continues to run on Quarkus. The teams agree on the A2A contract so they can choose their deployment runtimes independently.

Keep the incident, specialist role, inputs, impact result, and approval process consistent. Only the remote impact service changes runtime. The impact specialist assesses business cost and SLA risk; the main application still owns incident state, escalation approval, and resolution.

## What you will run

The Quarkus main application on **8080** delegates to an impact agent on **WildFly at 8888**. The WildFly application is a standalone WAR using LangChain4j CDI's portable extension. It shares no Panache entities or database with the caller.

The supplied HTTP adapter uses the A2A SDK to parse and serialize protocol messages. It advertises **non-streaming** operation and returns a synchronous agent message. It does not advertise streaming, background tasks, cancellation, or push notifications. This keeps the optional exercise focused on discovery and delegation across runtimes.

!!! note "What changes in the programming model?"
    The WildFly `ImpactAgent` remains a Java interface, with `@Agent`, `outputKey = "businessImpact"`, `@SystemMessage`, and `@UserMessage`. LangChain4j CDI registers it with `@RegisterSimpleAgent`; Quarkus uses its own extension integration. CDI creates the implementation in both cases. Do not implement the interface or move reasoning into the supplied servlet.

    LangChain4j CDI prompt placeholders use `{{service}}` with `@V("service")`. Keep that syntax in this module; it is not a replacement for the Quarkus examples' prompt syntax.

## 1. Prepare the local runtime (3 min)

Use **Java 25**, Python 3 for the optional offline smoke test, and a running **Podman or Docker** engine. No separate WildFly installation is needed.

On macOS or Windows, start Podman Desktop/its Podman machine, or Docker Desktop, before running the launcher. Check your chosen engine and Java:

```bash
java -version
podman info
# Or: docker info
```

Stop the Quarkus **remote** impact agent from Exercise 7 with `Ctrl+C`, freeing port **8888**. Keep the Quarkus main application if it is already running on **8080**.

Export `OPENAI_API_KEY` in the terminal that will start WildFly, as in the workshop prerequisites. The launcher passes the environment variable at runtime; it is not copied into the image.

## 2. Build and start WildFly (4 min)

From the repository root:

=== "Podman"

    ```bash
    cd solutions/07-a2a/wildfly-impact-agent
    CONTAINER_ENGINE=podman ./run.sh up
    ```

=== "Docker"

    ```bash
    cd solutions/07-a2a/wildfly-impact-agent
    CONTAINER_ENGINE=docker ./run.sh up
    ```

The launcher runs the module's tests using the existing workshop Maven wrapper, packages `target/ROOT.war`, builds an image from **`quay.io/wildfly/wildfly:42.0.0.Final-jdk25`**, and waits for AgentCard discovery to succeed. It publishes container port **8080** on host **127.0.0.1:8888**. The first build needs time to download the server image and Maven dependencies.

Verify discovery from your host terminal:

```bash
curl -fsS http://localhost:8888/.well-known/agent-card.json | jq
```

Check the `impact-agent` name, `impact-assessment` skill, JSON-RPC interface, and the public URL `http://localhost:8888/`. The streaming capability is false (or omitted when the SDK serializes default values). That capability difference is deliberate: callers negotiate a blocking invocation.

!!! tip "A container's port and its advertised URL are different"
    WildFly listens on port 8080 **inside** its container. The caller runs on the host and reaches it through port 8888. The card therefore advertises `http://localhost:8888/`, not the container's internal address. The supplied launcher sets `PUBLIC_AGENT_URL` to match its host-port mapping.

## 3. Delegate the same Apex incident (5 min)

If the Quarkus main application is stopped, start it in a second terminal from the repository root:

```bash
cd solutions/07-a2a/multi-agent-system
./mvnw quarkus:dev
```

Open [http://localhost:8080](http://localhost:8080), view incident **#1** (`payment-gateway` / `checkout-api`), and reuse the Exercise 7 report:

```text
Complete service outage, all API endpoints returning 503, cascading failures across dependent services
```

Process the incident and complete the existing human escalation approval when prompted. Open the main application's [execution report](http://localhost:8080/incident-management/report). Find the remote `impact-agent` call and its **Estimated Impact / Justification** result, followed by the local escalation and resolution steps.

Inspect the server runtime in a third terminal:

```bash
podman logs apex-wildfly-impact
# Or: docker logs apex-wildfly-impact
```

The logs should show WildFly starting, `ROOT.war` deploying, and the LangChain4j CDI portable extension registering `com.incidentmanagement.impact.ImpactAgent`. Incident reports and model credentials are not logged by the supplied adapter.

Compare the service boundaries with Exercise 7: the same Quarkus caller discovers the same capability at the same public URL. The remote team changed its runtime and CDI integration without rewriting the incident workflow. LLM-generated cost estimates can differ; compare the contract and workflow continuation, not exact wording or dollar amounts.

## 4. Inspect the agent and discuss ownership (3 min)

Read these files in `solutions/07-a2a/wildfly-impact-agent/`:

- `ImpactAgent.java` under `src/main/java/com/incidentmanagement/impact/`: the declarative specialist and its prompts.
- `ModelConfiguration.java`: a CDI producer for the OpenAI chat model, using environment configuration.
- `ImpactProtocol.java` and `ImpactEndpoint.java`: supplied SDK/HTTP transport plumbing.
- `Containerfile`: the WAR deployment into the official WildFly image.

Discuss with your co-presenter: which team owns the impact rubric and deployment? Which application owns approval? What does the caller need to know about the remote runtime? What failure does a stopped remote server introduce?

## Offline verification for facilitators

The repeatable smoke test starts a real WildFly container and a local scripted OpenAI-compatible HTTP server, then invokes the deployed agent using LangChain4j's A2A client and the existing Quarkus caller's own dependency graph. It checks that the business prompt and all four inputs, including a multiline description, reach the model. It does not call an external LLM or require a real API key.

```bash
cd solutions/07-a2a/wildfly-impact-agent
CONTAINER_ENGINE=podman python3 smoke.py
# Or: CONTAINER_ENGINE=docker python3 smoke.py
```

It uses port **18889** by default (`WF_SMOKE_PORT` overrides it) and stops its test container on exit. Run this before delivering the optional exercise. The normal launcher runs the protocol unit tests; the container interoperability tests run when `smoke.py` supplies `-Dwildfly.url`. The `WildFly optional exercise` GitHub Actions workflow runs the same smoke test with Docker.

## Stop or return to the Quarkus remote service

```bash
cd solutions/07-a2a/wildfly-impact-agent
CONTAINER_ENGINE=podman ./run.sh down
# Or: CONTAINER_ENGINE=docker ./run.sh down
```

The stopped container is removed automatically. To repeat the original Exercise 7 setup, restart `solutions/07-a2a/remote-a2a-agent` on port 8888. Stop the main Quarkus application before starting Exercise 8.

If startup fails, inspect `./run.sh logs` with the same engine selection. A healthy empty WildFly server is insufficient: the AgentCard must return HTTP 200, confirming that the WAR deployed. If port 8888 is occupied, stop the previous remote service. An alternate `WF_PORT` is available for isolated tests, but the standard Quarkus caller expects port 8888.

<div class="done-when" markdown>

## :material-check-circle: Done when

- [ ] WildFly runs the remote impact agent in a container on host port 8888.
- [ ] The AgentCard advertises a reachable URL and the impact-assessment skill.
- [ ] The Quarkus caller receives an impact result and continues through human approval.
- [ ] Riley's persona, Apex incident, and ownership of approval remain consistent.
- [ ] You can explain which runtime-specific wiring changed and which A2A contract stayed stable.

</div>

References: [LangChain4j CDI](https://github.com/langchain4j/langchain4j-cdi), [WildFly AI guide](https://www.wildfly.org/guides/langchain-ai-chat-bot/), [official WildFly container image](https://github.com/wildfly/wildfly-container), and [A2A Java SDK](https://github.com/a2aproject/a2a-java).
