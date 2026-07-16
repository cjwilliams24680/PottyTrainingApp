---
name: sync-graphql-schema
description: Pull the latest GraphQL schema from the PottyTrainingServer into the Android app via Apollo introspection, and regenerate the Kotlin models.
---

# Sync GraphQL Schema

Downloads the current GraphQL schema from a running PottyTrainingServer and regenerates
Apollo Kotlin models from it. Run this whenever the server's API changes — new fields, new
operations, changed nullability, new enum values.

Syncing is the easy part; the judgement is in reading the diff. Work through **Reading the
diff** and **Enum evolution** before shipping, because the changes that break already-installed
apps are exactly the ones that leave this build green.

## Background: why this is a manual step

The server is **code-first**: TypeScript decorators (`@ObjectType`, `@InputType`, resolvers)
generate `src/schema.gql` on boot, and that file is gitignored on the server side because it
is a build artifact there.

Android is **schema-first**: the schema is the *input* to codegen. Same file, opposite arrows.
It is the contract between the two repos, so this repo commits its copy — the diff on
`schema.graphqls` is the API changelog, and it is the only place a breaking server change
becomes visible in Android code review. Never hand-edit it.

## Prerequisites

The server must be running and reachable. It is a separate repo:

```bash
cd path/to/PottyTrainingServer
colima start && docker compose up -d   # Postgres runs via Colima, not Docker Desktop
npm run start:dev                      # → http://localhost:3000/graphql
```

Confirm before downloading — a 400 or connection refused here is the cause of most failures:

```bash
curl -s -X POST http://localhost:3000/graphql \
  -H 'Content-Type: application/json' \
  -d '{"query":"{ __schema { queryType { name } } }"}'
```

Expect `{"data":{"__schema":{"queryType":{"name":"Query"}}}}`.

## One-time Gradle setup

If `apollo { ... }` is already configured in the app module, skip to **Running the sync**.

```kotlin
// app/build.gradle.kts
plugins {
  id("com.apollographql.apollo") version "4.1.0"
}

dependencies {
  implementation("com.apollographql.apollo:apollo-runtime:4.1.0")
}

apollo {
  service("potty") {
    packageName.set("com.yourapp.graphql")

    // Custom scalar. Without this, every DateTime field (`timestamp`,
    // `createdAt`) generates as a raw String.
    mapScalar(
      "DateTime",
      "java.time.Instant",
      "com.apollographql.apollo.api.Adapters.InstantAdapter",
    )

    // Generate enums as sealed interfaces so unknown values keep their raw
    // string. Apollo's default is a plain enum class whose unknown case
    // discards the server's value — see Enum evolution below. Do not remove.
    sealedClassesForEnumsMatching.set(listOf(".*"))

    introspection {
      // localhost, NOT 10.0.2.2 — see Gotchas.
      endpointUrl.set("http://localhost:3000/graphql")
      schemaFile.set(file("src/main/graphql/schema.graphqls"))
    }
  }
}
```

## Running the sync

```bash
./gradlew downloadPottyApolloSchema    # task name derives from the service name: "potty"
git diff src/main/graphql/schema.graphqls
./gradlew build
```

Read the diff before building. It tells you whether this is an additive change or a breaking
one, which determines how much work follows.

## Reading the diff

**Compatibility is asymmetric, and which direction breaks depends on where the type sits.**
An *output* type (`PottyLog.type`, anything under `Query`/the payload of a mutation) is data the
server sends you. An *input* type (`CreateLogInput`, `UpdateLogInput`) is data you send it.
Loosening a rule is safe on output and breaking on input; tightening it is the reverse. Every
row below is an application of that one idea, so read the position first, then the change.

| Diff shows | Position | Means |
| --- | --- | --- |
| New type / field / operation | either | Safe. Nothing breaks; add fields to operations to use them. |
| Field removed / renamed | either | **Breaking.** Any operation naming it fails codegen — fix the operation. |
| `String!` → `String` (nullable) | output | **Breaking.** Kotlin type becomes `String?`; call sites must handle null. |
| `String!` → `String` (nullable) | input | Safe. A field you already send is now merely optional. |
| `String` → `String!` (required) | output | Safe. You get a stronger guarantee than you compiled against. |
| `String` → `String!` (required) | input | **Breaking.** Omitting it is now a validation error. |
| Enum gained a value | output | **Breaking at runtime, not compile time.** See below. |
| Enum gained a value | input | Safe. You simply never send the new value. |
| Enum lost a value | input | **Breaking.** Old clients still sending it get rejected. |

`PottyType` is used in **both** positions — `PottyLog.type` (output) and `CreateLogInput.type` /
`UpdateLogInput.type` (input) — so it is subject to both halves of the table at once. That is
what makes the next section worth reading in full.

## Enum evolution

`PottyType` (`PEE`/`POO`/`BOTH`) is the kind of enum that grows. Adding a value to it is a
breaking change for every already-shipped app, but **not** in the way people expect.

**Nothing crashes.** The generated response adapter calls `PottyType.safeValueOf(rawValue)`,
which is total — an unrecognized value falls back to a generated `UNKNOWN__` case. There is no
exception and no GraphQL error. The record still arrives with every other field intact: an
unknown enum degrades *one field*, not one log, and not the response.

**Name the `UNKNOWN__` branch. Never write `else ->`.**

```kotlin
// Bind the subject with `val type =` so the UNKNOWN__ branch smart-casts and
// `rawValue` is reachable — a bare `when (log.type)` will not smart-cast a property.
val label = when (val type = log.type) {
  PottyType.PEE  -> "Pee"
  PottyType.POO  -> "Poo"
  PottyType.BOTH -> "Both"
  is PottyType.UNKNOWN__ -> "Unsupported (${type.rawValue}) — update the app"
}
```

`UNKNOWN__` is a real case in the generated type, so exhaustiveness already forces you to
handle it — an `else ->` adds nothing and costs you the compiler. When `DIAPER` is added and you
regenerate, a named `UNKNOWN__` branch produces a compile error pointing at every `when` that
needs a `DIAPER` case; an `else ->` silently swallows it and ships "unsupported" to users
forever. The `else` trades a compile error you want for a runtime bug you won't notice.

Equally: never write `UNKNOWN__ -> error("unreachable")` to satisfy the compiler. It is not
unreachable, it is *the future*, and that line is the only realistic way this crashes.

**The write-back hazard — the actual bug.** With Apollo's default enum codegen the unknown case
is `UNKNOWN__("UNKNOWN__")`: the server's real value is discarded. Since it is an ordinary
member of the type, nothing stops it being passed to a mutation, where the adapter does
`writer.value(value.rawValue)` and sends the literal string `"UNKNOWN__"` — which the server
rejects. A user edits the note on a log whose type is newer than their app, and the mutation
fails on a field they never touched.

`sealedClassesForEnumsMatching.set(listOf(".*"))` in the Gradle block above is what prevents
this. It generates `UNKNOWN__PottyType("DIAPER")` instead, preserving `rawValue`, so the value
both displays meaningfully and round-trips back unchanged. This is why that line is not
optional.

**Aggregations skew silently.** A "4 pees, 2 poos this week" summary buckets unknown types into
whatever the `UNKNOWN__` branch does — usually nothing. No crash, wrong numbers, no signal.
Worth an explicit decision anywhere logs are counted or grouped rather than listed.

## Regenerating models

Codegen produces **one class per operation**, not one per schema type. The `.graphql` files in
`src/main/graphql/` are the definitions; the generated class contains exactly the fields the
operation selects.

```graphql
# src/main/graphql/GetLogs.graphql
query GetLogs {
  getLogs { id type timestamp isAccident note createdAt }
}
```

`./gradlew build` turns that into `GetLogsQuery`, `GetLogsQuery.Data`, `GetLogsQuery.GetLog`.
To use a newly added server field, add it to the selection set and rebuild — downloading the
schema alone changes no Kotlin.

## Gotchas

**`localhost` for the download, `10.0.2.2` for the runtime client.** The Gradle task runs on
the dev machine, so `endpointUrl` is `http://localhost:3000/graphql`. The app runs on an
emulator, where the host is reached at `10.0.2.2`, so `ApolloClient.Builder().serverUrl(...)`
uses `http://10.0.2.2:3000/graphql`. Two addresses, two contexts. Swapping them produces a
connection-refused that looks like the server is down when it isn't.

**Introspection is disabled in production.** The server does not set `introspection` explicitly,
so it inherits Apollo Server's default: on unless `NODE_ENV=production`. Against a production
deployment this task returns a 400 with an introspection-disabled error. That is intended —
sync against local dev or a staging box, never prod.

**Cleartext HTTP on the emulator.** `http://` to `10.0.2.2` requires a debug-only network
security config or the runtime request fails with a cleartext-not-permitted error. This
affects the app, not the download task.

**`mapScalar` is required or DateTime fields silently become `String`.** Apollo does not know
what a custom scalar means and falls back to the raw wire type. The build stays green; you just
get `String` where you wanted `Instant`, and you find out at the first call site. If a previously
working `Instant` field starts generating as `String`, check that the `mapScalar` line survived
a merge.

**Codegen failing after a sync is the system working.** A red build means the server dropped or
renamed something an operation depends on. Fix the `.graphql` operation; do not edit
`schema.graphqls` to make the error go away.
