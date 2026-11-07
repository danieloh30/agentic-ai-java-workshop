# Part 3 — Completed application

Use this one folder throughout Exercises 15–20. Start `./dev.sh`, open <http://localhost:8080>, and follow [Exercise 15](../../docs/15-runtime-skills/START_HERE.md). Keep Quarkus dev mode running for edits; Exercise 18 deliberately stops and restarts the JVM once while retaining the database volume.

[Setup, timeboxes, preflight and architecture boundaries](../README.md).

All six exercises are implemented. Follow the same experiment steps without adding duplicate test methods. In Exercise 20, make the candidate skill edit in this folder so baseline/candidate runs retain the same database and fixed dataset.

Run `./mvnw test` in a second terminal in this same folder. The scripted model and isolated test database make tests independent of paid model availability.

Preview requests allow one extra workflow invocation with explicit correction feedback when evidence is not quoted exactly from the operator report. The final evidence check still rejects invalid excerpts with a readable HTTP 422 message. The dashboard clears the previous recommendation when generating again. Run `node --test ../test/preview.test.cjs` to check the dashboard failure and retry behavior for both projects (Node.js required).
