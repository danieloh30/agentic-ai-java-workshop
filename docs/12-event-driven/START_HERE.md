# Exercise 12 — Event-Driven Agents (Capstone)

<span class="badge badge--code-along">Code-Along</span> <span class="badge" style="background:#1E5AA8;color:white;">Part 2 · Capstone</span>

**Timebox:** 20 minutes  
**Persona:** Jordan — Java platform engineer  
**You work in:** `solutions/12-event-driven/lab/`  
**Files to edit:**

- `src/.../agentic/workflow/IncidentResolutionWorkflow.java`
- `src/.../messaging/IncidentEventConsumer.java`

!!! tip "Full solution"
    If stuck, the completed files are in [`solutions/12-event-driven/`](https://github.com/danieloh30/agentic-ai-java-workshop/tree/main/solutions/12-event-driven){:target="_blank"} — copy them over your TODO files and restart.

---

## Enterprise context

**Jordan, Apex Systems' Java platform engineer, needs the incident platform to accept alerts while agent processing is still underway.** Monitoring integrations can report several failures close together, and an LLM-backed response takes longer than accepting an alert. Making each producer wait for triage and resolution couples alert ingestion to model latency and makes a dashboard's submission response carry two different meanings: accepted and completed.

Jordan needs an asynchronous path with a visible handoff between publication, agent processing, and result recording. This lab publishes incident events to Kafka, consumes them through the triage–resolution workflow, and sends results to a sink that updates the incident record. The UI acknowledges submission first; logs, the resolution lookup, and a refreshed dashboard show completion later. This separation demonstrates the event flow and its operational visibility, while the agents generate response recommendations rather than applying changes to external systems.

## The goal

Wire the sequential agent workflow into the Kafka consumer and publish an incident from the web UI. Follow the event through the consumer and resolution sink, then verify the saved result and `RESOLVED` status while distinguishing publication from completion.

## Why event-driven agents?

Every exercise so far triggered an agent with an **HTTP call** — you `POST`ed and waited for the answer. That's fine for a demo, but production incidents don't arrive as polite synchronous requests. They arrive as a *stream*: alerts fired by monitoring, events pushed onto a bus, faster than any human can click. And you don't want the alert source blocked while an LLM thinks for six seconds.

The capstone rewires the pipeline around **messaging**. An incident lands as an event on a Kafka topic; a consumer picks it up, runs the agentic pipeline, and publishes the resolution to a *second* topic — where a downstream sink consumes it and updates the database. The producer never waits for the agent. This is how you put agents into a real event-driven system: **agents as stream processors**.

### The pipeline

| Stage | Who does it | Built with |
|-------|-------------|------------|
| **Publish** | Turn a DB incident into an `IncidentEvent`, drop it on the bus | `@Channel` + `Emitter` (SmallRye Reactive Messaging) |
| **Consume + run** | Read the event, run triage→resolution, publish the result | `@Incoming`/`@Outgoing` + `@Blocking` |
| **The agent work** | Triage the incident, then decide the fix | `@SequenceAgent` over two `@Agent`s (Exercise 2) |
| **Sink** | Read the resolution, mark the incident RESOLVED | `@Incoming` + `@Transactional` |

```mermaid
%%{init: {'look':'handDrawn','theme':'neutral','themeVariables': {'lineColor':'#4A4035','fontSize':'18px'},'flowchart': {'nodeSpacing':40,'rankSpacing':50,'useMaxWidth':false}}}%%
flowchart TB
    API(["POST /publish/#123;id#125;"])
    T1[["incidents topic"]]
    CONS{{"@Incoming / @Outgoing<br/>consumer"}}
    SEQ(["@SequenceAgent<br/>Triage → Resolution"])
    T2[["resolutions topic"]]
    SINK(["@Incoming sink<br/>→ status RESOLVED"])
    DB[("PostgreSQL")]

    API -->|Emitter| T1
    T1 --> CONS
    CONS --> SEQ
    SEQ --> CONS
    CONS -->|"@Outgoing"| T2
    T2 --> SINK
    SINK --> DB

    style API fill:#E8DCC4,stroke:#6B5B45
    style T1 fill:#FFE4CC,stroke:#B87333
    style T2 fill:#FFE4CC,stroke:#B87333
    style CONS fill:#DCE8F0,stroke:#3D5A7A
    style SEQ fill:#D8F0D8,stroke:#3D7A3D
    style SINK fill:#DCE8F0,stroke:#3D5A7A
    style DB fill:#EDE0F0,stroke:#7A4A8A
```

!!! note "No Kafka to install — Dev Services provides it"
    The lab depends on `quarkus-messaging-kafka`. In dev mode, Quarkus **Dev Services** automatically starts a [Redpanda](https://redpanda.com/){:target="_blank"} broker (a Kafka-compatible engine) in a container and points the app at it — nothing to configure. You just need Docker or Podman running, the same as the PostgreSQL you've used all workshop.

---

## Step 0 — Start the lab (2 min)

```bash
cd solutions/12-event-driven/lab
export OPENAI_API_KEY=sk-your-key-here
./mvnw quarkus:dev
```

First launch is a little slower — Dev Services pulls both a PostgreSQL and a Redpanda image. Watch the log for the Kafka broker starting, then open [http://localhost:8080](http://localhost:8080){:target="_blank"}.

!!! warning "Expect a red screen until *both* steps are done"
    As in Exercise 11, the two TODO files are a unit. The `@SequenceAgent` workflow (Step 1) is what supplies the `@Agent`s their inputs, and the consumer (Step 2) is what actually runs the workflow. Until both are wired, dev mode shows a deployment error. Finish both and it boots green.

---

## Step 1 — Wire the agentic pipeline (7 min)

Open `IncidentResolutionWorkflow.java`. Two agents already exist:

- **`TriageAgent`** (`outputKey = "triage"`) — classifies the incident: category, severity, the next signal to check.
- **`ResolutionAgent`** (`outputKey = "resolution"`) — takes the incident **and** the triage, and decides the fix (starting with an action verb: ROLLBACK / SCALE_UP / RESTART / MONITOR).

Notice `ResolutionAgent.resolve(IncidentEvent, String triage)` takes `triage` as an explicit parameter — the `@SequenceAgent` binds it by name from the shared scope, exactly the sequencing pattern from Exercise 2.

Chain them with `@SequenceAgent`:

```java
    @SequenceAgent(
            outputKey = "resolutionResult",
            subAgents = { TriageAgent.class, ResolutionAgent.class })
    ResolutionEvent process(IncidentEvent incidentEvent);
```

Then implement the `@Output` — assemble the final `ResolutionEvent` from the scope:

```java
    @Output
    static ResolutionEvent output(AgenticScope scope) {
        IncidentEvent incident = scope.readState("incidentEvent", (IncidentEvent) null);
        String triage = scope.readState("triage", "");
        String resolution = scope.readState("resolution", "");
        return new ResolutionEvent(incident.incidentId(), triage, resolution);
    }
```

Add the imports:

```java
import com.incidentmanagement.agentic.agents.TriageAgent;
import com.incidentmanagement.agentic.agents.ResolutionAgent;
import com.incidentmanagement.model.IncidentEvent;
import com.incidentmanagement.model.ResolutionEvent;
import dev.langchain4j.agentic.declarative.Output;
import dev.langchain4j.agentic.declarative.SequenceAgent;
```

Still red — the workflow exists, but nothing runs it yet. That's Step 2.

---

## Step 2 — Turn the consumer into a stream processor (8 min)

Open `IncidentEventConsumer.java`. The workflow is already injected. Your job is to make one method both **consume** incidents and **produce** resolutions.

Replace the TODO body and add the three messaging annotations:

```java
    @Incoming("incidents-in")
    @Outgoing("resolutions-out")
    @Blocking
    public ResolutionEvent process(IncidentEvent event) {
        Log.infof("Processing incident %d from Kafka", event.incidentId());
        return workflow.process(event);
    }
```

Add the imports:

```java
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Outgoing;
import io.smallrye.reactive.messaging.annotations.Blocking;
import io.quarkus.logging.Log;
```

That's the whole event loop:

- **`@Incoming("incidents-in")`** — Quarkus subscribes this method to the incidents topic and calls it for every event. It auto-generates a Jackson deserializer for the `IncidentEvent` record.
- **`@Outgoing("resolutions-out")`** — whatever you `return` is serialized and published to the resolutions topic.
- **`@Blocking`** — this workflow waits synchronously for LLM responses, which can take seconds. The annotation explicitly runs the consumer on a worker thread, keeping the reactive event loop free to handle other work.

!!! note "Why make worker-thread execution explicit?"
    Quarkus already dispatches this synchronous method to worker threads by default. Here, `@Blocking` makes that requirement explicit; removing it would not automatically put this method on the event loop. See the [Quarkus messaging execution model](https://quarkus.io/guides/messaging/#execution-model){:target="_blank"} for how method signatures and annotations determine the execution thread.

Save. The red screen clears and dev mode boots **green**.

!!! note "Channels vs. topics"
    `incidents-in` and `resolutions-out` are *channel* names — logical wires inside the app. `application.properties` maps each channel to a real Kafka topic (e.g. `mp.messaging.incoming.incidents-in.topic=incidents-in`). The REST resource's emitter, this consumer, and the downstream sink are three separate channels stitched onto two topics. Open `application.properties` to see the full wiring.

---

## Step 3 — Test the event pipeline in the web UI (3 min)

Open [http://localhost:8080](http://localhost:8080){:target="_blank"} and publish an incident from the dashboard:

1. Click **incident #2** — the P1 `auth-service / user-login` failure.
2. Enter this report:

    ```text
    Total login failure since 14:00; auth pods OOMKilled after last deploy
    ```

3. Click **Process Incident**. The panel closes and a success notification appears after the event is submitted. That confirms publication; the agents continue working asynchronously, so the incident may still show **In Progress**.

Watch the **terminal logs** for the stages:

```text
Published incident #2 to incidents-in (from dashboard)
Processing incident 2 from Kafka
Pipeline resolved incident #2 → publishing to resolutions-out
Recorded resolution for incident #2 and marked it RESOLVED
```

Once the sink records the resolution, **refresh the dashboard** and confirm that #2 is **Resolved**. The dashboard refreshes once after submission; it does not automatically poll for completion. Processing time depends on the model and broker, so use the completion log rather than a fixed six-second delay.

Open [incident #2's resolution](http://localhost:8080/incident-events/resolutions/2){:target="_blank"} in another browser tab to read the `triage` and `resolution` fields. The incident's Description remains its original report; the result is available at this link. You can also open [all resolutions from this session](http://localhost:8080/incident-events/resolutions){:target="_blank"}.

!!! tip "If the resolution page returns 404"
    The sink has not recorded the result yet. Wait for the completion log, then refresh the resolution page. A submission can succeed before the resolution exists — that is the asynchronous behavior this exercise demonstrates.

??? info "Advanced — Run again and inspect JSON"

    The web UI tests above are sufficient to complete this exercise. **Keep Quarkus running; no restart is required.** The POST request below publishes a new event and reruns the pipeline. To inspect the result from your UI test without publishing again, skip the POST and use the resolution and incident-status GET requests below.

    The API offers the same publish-then-read flow:

    ```bash
    curl -s -X POST "http://localhost:8080/incident-events/publish/2" \
      -H "Content-Type: text/plain" \
      --data "Total login failure since 14:00; auth pods OOMKilled after last deploy" | jq
    ```

    ```json
    { "published": true, "incidentId": 2, "topic": "incidents-in", "next": "GET /incident-events/resolutions/2 (once the pipeline finishes)" }
    ```

    If you publish again, watch the terminal logs for the consumer and sink. Once the sink records the resolution, read it with:

    ```bash
    curl -s "http://localhost:8080/incident-events/resolutions/2" | jq
    ```

    ```json
    {
      "incidentId": 2,
      "triage": "Category: Configuration / Severity: Critical / Next signal: recent deploy diff",
      "resolution": "ROLLBACK the last deployment immediately; the OOMKills began right after it ..."
    }
    ```

    After the round trip — publish → Redpanda → consumer → triage → resolution → resolutions topic → sink — the sink also sets the incident to **RESOLVED** in PostgreSQL. Check it with:

    ```bash
    curl -s "http://localhost:8080/incidents" | jq '.[] | select(.id==2) | {id, status}'
    ```

    ```json
    { "id": 2, "status": "RESOLVED" }
    ```

    Try `GET /incident-events/resolutions` (no id) to see every resolution the pipeline has produced this session.

    Publishing again reruns the pipeline for the same incident. If a resolution already exists, the lookup can return that previous result while the new event is processing; watch the logs to identify completion of the new run.

---

<div class="done-when" markdown>

## :material-check-circle: Done when

- [ ] Submitted incident #2 through the web UI; you can explain why the success notification confirms publication before processing finishes
- [ ] Terminal logs show the Kafka consumer processing the incident and the sink recording its resolution
- [ ] After the sink completes, the refreshed dashboard shows `RESOLVED` and the resolution page contains both `triage` and `resolution`
- [ ] You can explain from memory: how `@Incoming` and `@Outgoing` connect the pipeline, how channels map to Kafka topics, and why LLM calls use `@Blocking`

</div>

## What you learned — and the whole workshop

- **Agents as stream processors.** An `@Incoming`/`@Outgoing` method turns your agentic pipeline into a Kafka consumer that reads events and publishes results — decoupled from whoever produced the event.
- **`@Blocking`** keeps slow LLM calls off the reactive event loop, so the broker and the rest of the app stay responsive.
- **Dev Services** gave you a real Kafka broker (Redpanda) with zero configuration — the same frictionless loop you've had for PostgreSQL all workshop.
- **The agentic patterns compose.** The `@SequenceAgent` here is the same primitive from Part 1; the capstone just changed its *trigger* from HTTP to an event and its *output* from a response body to another topic.

You've now gone from a single agent with one tool (Exercise 1) to a **fully event-driven, multi-stage agentic system** running on Quarkus — planning, memory, consensus, and streaming included. That's the arc of the workshop.

??? tip "Stretch goals"
    - Add a **dead-letter** path: when resolution fails, `@Outgoing` to a `failures` topic instead, and consume it with a retry sink.
    - Publish the *consensus* vote from Exercise 11 as the resolution decision — swap the `@SequenceAgent` for the `@ParallelAgent` voting workflow behind the same consumer.
    - Emit an event per *stage* (triaged, resolved) so a UI can show live progress as the pipeline runs.

🎉 **That's the capstone — you've completed the workshop.** Head back to the [Home page](../index.md) for the full exercise map, or revisit any pattern in `solutions/`.
