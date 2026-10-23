# Part 3 — Cumulative starter

Use this one folder throughout Exercises 15–20. Start `./dev.sh`, open <http://localhost:8080>, and follow [Exercise 15](../../docs/15-runtime-skills/START_HERE.md). Keep Quarkus dev mode running for edits; Exercise 18 deliberately stops and restarts the JVM once while retaining the database volume.

[Setup, timeboxes, preflight and architecture boundaries](../README.md).

The starter is runnable with conservative defaults: invalid recommendations fail immediately, scheduled planning is not connected until Ex17, and abandoned-attempt recovery is added in Ex18. Tests for the supplied behavior pass before edits. Each guide shows a focused change; add its regression test as you go. Do not copy/reset the whole project between exercises.

Run `./mvnw test` in a second terminal in this same folder. The scripted model and isolated test database make tests independent of paid model availability.
