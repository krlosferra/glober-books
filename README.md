# GloberBooks

A personal reading-list tracker (REST API and a lightweight web frontend) built with Spec-Driven Development: the specification drives the code, and every phase is reviewed before the next one starts.

## Start here
- `docs/sdd/README.md`: map of the project documents, who owns each, and how to find things.
- `docs/sdd/status.md`: live board (what is done, in progress and blocked).
- `docs/sdd/constitution.md`: the rules every change follows (stack, pinned versions, commands, security).
- `CLAUDE.md`: instructions for the automated agents that work in this repository.

## First steps in a new clone
1. `git config core.hooksPath .githooks` (enables the commit and push guards).
2. Configure your own identity: `git config user.name` and `git config user.email`.
3. Read `docs/sdd/git-workflow.md` before your first commit.


## Prerequisites
- JDK 21, 21.0.12 or a later 21.0.x patch (any OpenJDK-based distribution), with `JAVA_HOME` or `java` on the `PATH`.
- Node 24.22.2 (see `.nvmrc`), used only for the tests.
- Nothing else: Maven comes from the wrapper in `backend/`, and the frontend has no build step.

## Build
```
cd backend && ./mvnw verify
```
This runs the tests and builds `backend/target/globerbooks.jar`.

## Run
The database lives in `backend/data/`. The application does not create that directory yet, so create it once from the repository root:
```
mkdir -p backend/data
```
Then start the application in one of two ways (the memory bound is confirmed or changed later in the project):
```
cd backend && ./mvnw spring-boot:run -Dspring-boot.run.jvmArguments=-Xmx512m
```
or, after the build, the jar (`backend/target/globerbooks.jar`):
```
cd backend && java -Xmx512m -jar target/globerbooks.jar
```
Run both from `backend/`: the database path `./data/globerbooks.db` is relative to the working directory, so from any other directory the start fails with "unable to open database file".
Open the page at `http://127.0.0.1:8080/`. The application listens on the loopback address only. Stop it with `Ctrl+C`.

The database file is locked exclusively while the application runs: no other tool (no `sqlite3`, no viewer) can open it. Stop the application first.

`v0.1` and `v0.2` are checkpoints for trying the project, **not for real data**. Their database (`backend/data/globerbooks.db`) is deleted before Increment 1.

## Tests
Logic tests of the frontend (from the repository root):
```
node --test "frontend/**/*.test.js"
```
End-to-end tests (Playwright). One-time setup, then the smoke run:
```
cd e2e && npm ci --ignore-scripts
npx playwright install chromium
npx playwright test --grep @smoke
```
The suite needs the built jar. Playwright starts the application itself on `127.0.0.1:18080` with a temporary database, so port 18080 must be free; it does not touch `backend/data/`. The full suite is `npx playwright test`. The hostile-environment check is run only on request:
```
npx playwright test --grep @isolation
```
