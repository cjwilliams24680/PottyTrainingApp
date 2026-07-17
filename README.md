# Potty Training Tracker

An Android app for logging a child's potty-training events — built as a portfolio project to demonstrate production-grade Android engineering on a deliberately small problem.

The app is a Kotlin + Jetpack Compose client backed by a companion [NestJS GraphQL server](https://github.com/cjwilliams24680/PottyTrainingServer), with Room acting as a local cache of server state and a complete auth flow (signup, login, token refresh, logout). It was developed with an AI-assisted workflow, and the repository itself is structured so that both human and AI collaborators onboard from the same documented contracts.

## Screenshots

<img width="323" height="720" alt="Screen_recording_20260717_163419" src="https://github.com/user-attachments/assets/49127c70-80a2-40fd-9b9d-96497f90f153" />

## Tech stack

| Area | Choice |
|---|---|
| Language / UI | Kotlin 2.1, Jetpack Compose (Material 3), type-safe Navigation Compose with `@Serializable` routes |
| Architecture | Three-layer clean architecture (domain / data / ui), MVVM, unidirectional data flow |
| DI | Hilt, with codegen through KSP (not kapt) |
| Networking | Apollo Kotlin 5 (GraphQL), custom auth interceptors |
| Persistence | Room 2.8 (as a server-state cache), DataStore (token storage) |
| Async | Coroutines + Flow end to end |
| Testing | 62 unit tests — JUnit, Turbine, coroutines-test, Apollo testing support, hand-rolled fakes |
| Tooling | Gradle version catalog, Timber (debug-only), core library desugaring for `java.time` on minSdk 24 |

## Architecture

Three layers under [`app/src/main/java/com/cjwilliams/pottytraining/`](app/src/main/java/com/cjwilliams/pottytraining/), with dependencies pointing inward to `domain`:

- [`domain/`](app/src/main/java/com/cjwilliams/pottytraining/domain/) — pure Kotlin: models, repository interfaces, and a sealed [`AppResult`/`AppError`](app/src/main/java/com/cjwilliams/pottytraining/domain/AppResult.kt) result type used at every boundary. No Android, Room, or Compose imports.
- [`data/`](app/src/main/java/com/cjwilliams/pottytraining/data/) — Room + GraphQL. Mapping happens at the edges, so neither Room entities nor Apollo-generated types ever escape this layer.
- [`ui/`](app/src/main/java/com/cjwilliams/pottytraining/ui/) — one package per feature (auth, create/edit log, history, settings), each holding its screen, view model, and extracted composables.

The committed [`schema.graphqls`](app/src/main/graphql/schema.graphqls) is the cross-repo API contract with the server.

## Engineering highlights

The interesting decisions, each with where to look in the code.

### Single-flight token refresh

Concurrent requests hitting an expired token should trigger *one* refresh, not a stampede. [`TokenManager`](app/src/main/java/com/cjwilliams/pottytraining/data/auth/TokenManager.kt) guards refresh with a `Mutex` and compares which access token each caller used, so racing callers share a single refresh — and the session is only cleared on a genuine auth failure, never a network blip. [`NetworkModule`](app/src/main/java/com/cjwilliams/pottytraining/di/NetworkModule.kt) provides two Apollo clients (an authenticated one and an unauthenticated one for the refresh call itself) to avoid interceptor recursion, and `TokenManager` is injected via `Provider` to break a Hilt dependency cycle.

A subtlety: the server reports auth failures as HTTP 200 with an `UNAUTHENTICATED` GraphQL extension, not a 401 — so [`AuthRetryInterceptor`](app/src/main/java/com/cjwilliams/pottytraining/data/auth/AuthRetryInterceptor.kt) lives at the Apollo layer (where GraphQL errors are visible) rather than the HTTP layer, refreshing once and replaying the operation.

### Forward-compatible enum handling

If the server adds a new enum value, an old client must not corrupt data. The Apollo config in [`app/build.gradle.kts`](app/build.gradle.kts) uses `sealedClassesForEnumsMatching` so unknown enum values retain their raw string instead of collapsing into a lossy `UNKNOWN__` constant that would get written back to the server. In [`GraphqlMappers`](app/src/main/java/com/cjwilliams/pottytraining/data/remote/GraphqlMappers.kt), rows with an unrecognized `PottyType` are dropped rather than guessed.

### The cache is a projection, not a source of truth

[`PottyRepositoryImpl`](app/src/main/java/com/cjwilliams/pottytraining/data/PottyRepositoryImpl.kt) writes to the server first and updates Room from the server's returned copy, so the two can't drift. `refreshLogs()` only replaces the cache on success — stale data beats an empty screen. Because Room is strictly a cache, [`DatabaseModule`](app/src/main/java/com/cjwilliams/pottytraining/di/DatabaseModule.kt) deliberately uses destructive migration: schema changes drop and refetch rather than accumulating migration code for data the server already owns. (The caveat — revisit if local-only data ever ships — is documented in the repo.)

### Explicit API semantics: "clear" vs "leave unchanged"

Update mutations distinguish `Optional.Present(null)` (clear this field) from `Optional.Absent` (don't touch it) in [`GraphqlMappers`](app/src/main/java/com/cjwilliams/pottytraining/data/remote/GraphqlMappers.kt) — a distinction that partial-update APIs frequently get wrong.

### A deliberate ViewModel state pattern

Every screen follows the same shape, converged on intentionally (visible in the commit history): sealed-interface UI state (`Uninitialized` / `Loading` / `Loaded` / `BlockingError`) exposed as a `StateFlow`, with one-shot events (e.g. navigate after save) on a separate `SharedFlow` — state is for rendering, events fire exactly once. See [`PottyLogViewModel`](app/src/main/java/com/cjwilliams/pottytraining/ui/createlog/PottyLogViewModel.kt). [`HistoryViewModel`](app/src/main/java/com/cjwilliams/pottytraining/ui/history/HistoryViewModel.kt) combines the repository flow with refresh status to distinguish "empty because still loading" from "confirmed empty" from "refresh failed but cached data exists".

### Type-safe navigation and session-driven graphs

Routes are a `@Serializable` sealed interface in [`Route.kt`](app/src/main/java/com/cjwilliams/pottytraining/Route.kt) — `composable<Route.EditLog>` and `toRoute<>()`, no string routes. [`MainActivity`](app/src/main/java/com/cjwilliams/pottytraining/MainActivity.kt) swaps the entire nav graph on session state: unknown → splash, logged out → auth graph, logged in → main scaffold with bottom nav.

### Tested where it counts

62 unit tests cover the layers where logic actually lives: repository write-through and refresh behavior, GraphQL mappers, the auth retry interceptor, and every view model — using hand-rolled fakes and [Turbine](https://github.com/cashapp/turbine) for Flow assertions rather than heavyweight mocking.

## Development workflow

This repo is set up for AI-native development, treated as an engineering discipline rather than autocomplete:

- [`CLAUDE.md`](CLAUDE.md) is maintained as an executable contract for the repo — architecture rules, the "adding a screen touches three places" checklist, and conventions — so AI agents and new humans onboard from the same source of truth.
- A custom Claude Code skill, [`sync-graphql-schema`](.claude/skills/sync-graphql-schema/SKILL.md), automates pulling the server's schema via Apollo introspection and regenerating Kotlin models — including written guidance on reading schema diffs for breaking changes and why codegen failures after a sync are the system working as intended.
- Non-obvious decisions carry comments explaining *why* (the single-flight refresh, the enum strategy, stale-cache-beats-error), which pays off equally for human reviewers and AI tools.

## Building & running

Open in Android Studio and run the `app` configuration, or:

```bash
./gradlew assembleDebug        # build debug APK
./gradlew testDebugUnitTest    # run the unit test suite
```

Server-backed features need the [companion server](https://github.com/cjwilliams24680/PottyTrainingServer) running locally on port 3000 — the emulator reaches it at `10.0.2.2:3000`.
