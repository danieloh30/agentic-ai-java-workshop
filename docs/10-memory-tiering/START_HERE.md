# Exercise 10 — Memory Tiering and Eviction

<span class="badge badge--code-along">Code-Along</span> <span class="badge">Part 2 · 15 minutes</span>

**Persona:** Sam — NOC analyst<br>
**You work in:** `solutions/10-memory-tiering/lab/`<br>
**File to edit:** `src/main/java/com/incidentmanagement/agentic/agents/IncidentAssistant.java`.

## Enterprise context and goal

Sam needs conversation continuity without mixing incident reports or sending an unbounded transcript to the model. This lab separates **the bounded message window sent to the model** from **the PostgreSQL store that persists that window**.

The store is supplied. Wire and inspect `@MemoryId` and `@ChatMemoryProviderSupplier`, then change one window-size setting and observe eviction. **Evicted messages leave the database too. This is not a full conversation archive.**

## Step 0 — Start green (2 min)

Stop Exercise 9. From the repository root:

```bash
cd solutions/10-memory-tiering/lab
export OPENAI_API_KEY=your-key
./mvnw quarkus:dev
```

Open [the dashboard](http://localhost:8080). The starter already has a working `@Agent`, a six-message window, and a transactional `PersistentChatMemoryStore`. No initial build error is expected.

## Step 1 — Inspect the wiring, then change the window (4 min)

Open `IncidentAssistant.java` and find:

```java
@Agent(description = "Conversational assistant that reasons about a single incident with memory of the conversation so far.",
       outputKey = "assistantReply")
String chat(@MemoryId Integer incidentId, @UserMessage String message);
```

`@MemoryId` selects the conversation. The supplied static `@ChatMemoryProviderSupplier` builds:

```java
return MessageWindowChatMemory.builder()
        .id(memoryId)
        .maxMessages(6)
        .chatMemoryStore(store)
        .build();
```

The six-message cap includes the system message, which is retained as other messages are evicted. Messages are not turns, and a message cap is not a token cap. The CDI-managed store reads and writes the retained list as JSON in PostgreSQL.

Change `maxMessages(6)` to **`maxMessages(3)`**. Save and allow dev mode to reload **before starting the conversation**. The system message and latest user/AI pair now fill the window. The store implementation is supplied so you can spend the time observing the boundary.

## Step 2 — Observe eviction and isolation (7 min)

1. Open incident **#2** and submit `The pods are OOMKilled every five minutes. Where do I start?`.
2. Open [#2's stored history](http://localhost:8080/incident-assistant/2/history) in another tab. The history endpoint shows message count and types, not text.
3. In **Continue Conversation**, send `I am checking the dashboard now. Acknowledge briefly.` Then send `What symptom did I first report?`.
4. Inspect `chatmemoryentity` in the [Dev UI database view](http://localhost:8080/q/dev-ui/quarkus-agroal/database-view). With three messages retained, the original operator message has been evicted. An answer may still repeat a symptom from later replies or the incident's seeded description; judge eviction from the stored messages, not answer wording alone.
5. Open incident **#4** and ask `What symptom did I report earlier in this conversation?`. Its own seeded description is available, but #2's operator conversation must not appear in its history. Compare [#4's history](http://localhost:8080/incident-assistant/4/history).

The dashboard keeps replies in browser memory; refreshing clears the displayed replies. The history endpoint and database are the evidence of server-side storage.

## Step 3 — Explain the tradeoff (2 min)

Restore `maxMessages(6)` or try `20`. Increasing the cap does **not** recover messages already removed from the database. A larger window can retain more context at greater request size; historical retrieval or summaries require separate design.

## Done when

- [ ] You changed the window and verified eviction in PostgreSQL.
- [ ] You verified that incident IDs select separate conversations.
- [ ] You can explain why this store persists a bounded window rather than an unlimited transcript.
- [ ] You distinguish browser refresh from application restart and database retention.

## Supplied verification and optional challenges

`./mvnw test` uses scripted model responses and PostgreSQL Dev Services. It verifies eviction removes messages from storage, memory can be reloaded from the database, incident isolation, and prior retained messages reaching the agent's next prompt. It makes no paid model calls.

**Optional restart experiment, outside the 15-minute core:** use a dedicated retained PostgreSQL database and start with a persistent schema strategy, for example `quarkus.hibernate-orm.schema-management.strategy=update` and `quarkus.hibernate-orm.sql-load-script=no-file`. Configure the datasource URL and credentials, create an incident through the UI, chat, restart, and check its history. Do not use the supplied destructive seed import for this experiment. A fresh Dev Services database does not prove restart persistence.

Other optional challenges: add a forget endpoint or design a separate archive/summary. **Full solution:** the parent `solutions/10-memory-tiering/` project. Stop this application before [Exercise 11](../11-consensus-voting/START_HERE.md).
