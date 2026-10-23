# Exercise 19 — Deterministic Regression Tests

<span class="badge badge--code-along">Code-Along</span> <span class="badge">Part 3 · 10 minutes</span>

**Persona:** Jordan — Java platform engineer<br>
**Continue in:** `part3/lab/` (or the completed `part3/solution/`), **Tests & evaluation** workspace.<br>
**File to edit:** `src/test/java/com/incidentmanagement/ResponseTest.java`.

## Why this exercise

A live demo can look correct while missing rare failures. Jordan needs repeatable checks of skill wiring, bounded repairs, correlation and persisted state, independently of paid model availability.

## Step 1 — Run the existing checks (3 min)

In a second terminal **in the same shared project**, run:

```bash
./mvnw test
```

Keep dev mode running. `ScriptedModel` starts a local HTTP server that speaks the OpenAI chat-completions protocol. Tests boot Quarkus and exercise the generated `@Agent` implementations, not hand-written mocks of their Java interfaces. The test profile starts an isolated PostgreSQL Dev Services database and disables the scheduler so each test controls execution.

The tests never use your paid API key. The scripted model verifies behavior under chosen responses, not the quality of the live model.

## Step 2 — Inspect the skill round trip (2 min)

Open `nativeSkillToolRoundTrip` in `ResponseTest`:

1. The first scripted response requests native `activate_skill` with `skill_name=incident-response`.
2. The framework executes the actual skill provider.
3. The next outbound model request contains the tool result and skill instructions.
4. The final scripted response satisfies the recommendation contract.

This proves skill loading and tool wiring. It does not prove a live model will always choose to activate the skill. Inspect `guardrailRepromptsInvalidContract` from Exercise 16 to see how a deterministic invalid response exercises runtime enforcement.

## Step 3 — Add an adversarial regression (5 min)

Add this method inside `ResponseTest`:

```java
@Test
void wellFormedButUnrelatedEvidenceCannotReachApproval() throws Exception {
    var r = store.create(id(), 1L, REPORT);
    store.claim();
    var invented = mapper.readValue(
        VALID.replace("Checkout 503 errors", "CPU is 99%"),
        Recommendation.class);
    assertTrue(RecommendationChecks.violations(invented).isEmpty());
    store.complete(r.id, 1, invented);
    assertEquals("FAILED", store.get(r.id).status);
    assertNull(store.get(r.id).proposalHash);
}
```

Run `./mvnw test` again. The fixture is valid JSON and passes the structural contract, yet its evidence belongs to no supplied report fact. This test prevents a future refactor from accidentally treating schema validity as grounding.

If time permits, temporarily use a known report excerpt instead of `CPU is 99%`, rerun just this test, observe its assertion fail, then restore the unrelated evidence. This checks that the test distinguishes the two cases rather than passing regardless of the input.

The solution already includes the added regression; run it without adding a duplicate method. Additional optional cases include whitespace-only checks and late completions from previous attempts.

## Done when

- [ ] Tests pass without paid model calls or touching your dev-mode database.
- [ ] You can explain what the skill round-trip test proves and what it cannot prove.
- [ ] Unrelated evidence cannot reach human approval despite a valid response contract.

[Next: Exercise 20 — Quality Evaluation](../20-quality-evaluation/START_HERE.md).
