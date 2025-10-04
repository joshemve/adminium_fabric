# Repository Guidelines

## Project Structure & Module Organization
Core mod code lives in `src/main/java/com/adminium/mod/**`, grouped by domain (commands, client, network, data, world). Gameplay assets and `mods.toml` sit under `src/main/resources`, while generated assets flow into `src/generated/resources` (kept in sync via `./gradlew runData`). Dev run profiles write saves/configs to `run/` (client/server) and `run-data/`. Release jars are produced in `build/libs/`. Versioning, mappings, and Forge coordinates are centralized in `gradle.properties`; update those instead of editing Gradle literals.

## Build, Test, and Development Commands
- `./gradlew build` – compiles, runs unit tests, and produces a reobfuscated jar in `build/libs/`.
- `./gradlew runClient` – launches a dev client using the `run/` workspace.
- `./gradlew runServer` – spins up a headless dev server with Adminium enabled.
- `./gradlew runData` – regenerates data-driven assets into `src/generated/resources/`.
- `./gradlew test` – executes any JVM tests under `src/test/java`.
- `./gradlew runGameTestServer` – runs Forge GameTests for automated gameplay validation.

## Coding Style & Naming Conventions
Target Java 17 with four-space indents and braces on the same line as declarations. Follow package-per-feature grouping (`client`, `command`, `network`, `data`). Classes use PascalCase, methods/fields use camelCase, and constants stay in UPPER_SNAKE_CASE. Prefer `Component.translatable` over hard-coded strings when messaging players. Keep networking logic in `network` and guard all player casts (`instanceof ServerPlayer`). Run `./gradlew build` before submitting to ensure resources expand correctly.

## Testing Guidelines
Add JVM tests beneath `src/test/java` using JUnit 5 (Gradle provides the harness) for pure logic. For in-game behavior, create Forge GameTests tagged with `@GameTest` under a `<feature>/test` package so they can run via `./gradlew runGameTestServer`. Name test classes after the feature under test (`FreezeCommandTest`, `TeamRolesGameTest`) and document setup expectations in comments. Ensure new commands and GUIs include at least one regression test path.

## Commit & Pull Request Guidelines
Use concise, imperative subject lines from Git history as a model (`Add sendCommandFeedback support`, `Fix creative flight loss`). Group related changes into a single commit when possible. PRs should include: a feature summary, reproduction or verification steps, linked issues, and screenshots or short clips for UI or rendering updates. Call out data/reg config changes so reviewers can refresh generated resources.
