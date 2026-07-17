# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Single-module Android app (`:app`) for logging a child's potty training events. Kotlin + Jetpack Compose, Room, Hilt, type-safe Navigation Compose.

## Build & test

No JDK is on the shell `PATH` — Gradle fails with "Unable to locate a Java Runtime" unless you export Android Studio's bundled JBR first:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
```

With that set:

```bash
./gradlew testDebugUnitTest                 # local JVM unit tests
./gradlew testDebugUnitTest --tests "com.cjwilliams.pottytraining.ui.history.HistoryViewModelTest"   # single test class
./gradlew assembleDebug                     # build debug APK
./gradlew lint                              # Android lint
./gradlew connectedDebugAndroidTest         # instrumented tests (needs a running emulator/device)
```

The unit test suite lives under `app/src/test` and covers the repository, GraphQL mappers, auth interceptors, and view models, using hand-rolled fakes and Turbine for Flow assertions.

## Architecture

Three layers under `app/src/main/java/com/cjwilliams/pottytraining/`, with dependencies pointing inward to `domain`:

- **`domain/`** — pure Kotlin. `PottyLog`, `PottyType`, and the `PottyRepository` interface. Keep this free of Room, Android, and Compose imports.
- **`data/`** — Room implementation. `PottyEntity` is the storage type and owns the mapping (`toDomain()` / `toEntity()` extensions); `PottyRepositoryImpl` maps at the boundary so entities never escape this package. Note `PottyType` is persisted as a `String` and reconstructed with `valueOf`, so renaming an enum constant breaks existing rows.
- **`ui/`** — one package per feature (`createlog`, `history`, `settings`), each holding its screen, view model, and any extracted composables.

Hilt wires it together: `DatabaseModule` provides the Room database and DAO, `RepositoryModule` `@Binds` the repository interface to its impl. Both install into `SingletonComponent`. `PottyApplication` is `@HiltAndroidApp`; `MainActivity` is `@AndroidEntryPoint`; view models are `@HiltViewModel` and obtained via `hiltViewModel()`.

### Navigation

Routes are `@Serializable` types in a sealed `Route` interface (`Route.kt`) and navigated type-safely — `composable<Route.EditLog>` with `backStackEntry.toRoute<Route.EditLog>()`, not string routes. Adding a screen means touching three places:

1. `Route.kt` — add the `@Serializable` type, plus an entry in `TOP_LEVEL_ROUTES` if it belongs in the bottom nav bar.
2. `MainActivity`'s `NavHost` — add the `composable<...>` block.
3. `NavDestination.getTitle()` at the bottom of `MainActivity` — maps each route to its top app bar title string resource. A route missing here renders with no top bar.

`TOP_LEVEL_ROUTES` also drives back-arrow visibility: destinations in that list are treated as top-level and get no up button.

### View model state pattern

Follow the shape established in `PottyLogViewModel` (see recent commits — this was deliberately converged on):

- UI state is a **sealed interface** (`Uninitialized` / `Loading` / `Loaded` / `BlockingError`) exposed as a `StateFlow` via `MutableStateFlow` + `asStateFlow()`. Screens collect with `collectAsStateWithLifecycle()` and `when` over the state.
- Field edits go through `updateLoadedState { it.copy(...) }`, which no-ops unless the state is `Loaded`. Don't reach into `_uiState.value` and cast.
- **One-shot events** (navigation after a save) use a separate `MutableSharedFlow` (`saveEvent`), not state. State is for rendering; events are for things that must fire exactly once.
- `initialize(id)` is guarded by an `Uninitialized` check so re-composition doesn't reload.

`PottyLogScreen` serves both create and edit via a nullable `logId` — null means create. `HistoryViewModel` shows the simpler read-only variant: map the repository `Flow` and `stateIn(WhileSubscribed(5000))`.

### Conventions

- User-facing strings live in `res/values/strings.xml` and are read with `stringResource(...)`; literals in composables were deliberately migrated away from.
- Logging is Timber (`Timber.d(...)`), planted debug-only in `PottyApplication`.
- Dependencies are declared in the `gradle/libs.versions.toml` version catalog — add there and reference as `libs.*`, never hardcode a coordinate in `app/build.gradle.kts`.
- Room and Hilt code generation runs through KSP, not kapt.

### Database migrations

There are none, deliberately. The Room database is treated as a local cache of server state, not a source of truth, so `DatabaseModule` builds it with `fallbackToDestructiveMigration(dropAllTables = true)`. A schema change means bumping `version` in `PottyDatabase` and letting the old data be dropped and refetched — don't write `Migration` classes or turn on `exportSchema`.

This holds only as long as the database stays a cache. If anything is ever stored locally that the server doesn't have, this needs to be revisited before that data ships.
