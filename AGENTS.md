# loxone-client-kotlin

Kotlin Multiplatform library implementing the Loxone™ miniserver communication protocol.
Published as `cz.smarteon.loxone:loxone-client-kotlin` on Maven Central.

## Commands

```bash
./gradlew check                     # full verification (tests + detekt)
./gradlew jvmTest                   # fast feedback, main target
./gradlew detekt                    # lint, fails on any issue (detekt.yml)
./gradlew koverXmlReport            # code coverage
./gradlew publishToMavenLocal       # local publish
./gradlew dokkaHtml                 # generate API docs
./gradlew jvmAcceptanceTest         # needs real miniserver — see below
./gradlew release -Prelease.versionIncrementer=incrementPatch
```

## Build

- **JDK 21 required** for build; produces JVM 17 bytecode.
- Version catalog at `gradle/libs.versions.toml` — never hardcode versions.
- Native tests (`linuxX64Test`, `linuxArm64Test`) and JS browser tests are **disabled**.
- Ktor engine: CIO on JVM/Linux, JS engine on browser.

## Package convention

- Source package: `cz.smarteon.loxkt` (files are flat under `src/**/kotlin/`, not mirroring package paths).
- Detekt enforces `rootPackage: cz.smarteon.loxkt`.
- Wildcard imports allowed only for `io.ktor.*`.
- `kotlin.code.style=official`.

## Testing

- Framework: **Kotest** v6. Specs used: `ShouldSpec` (preferred), `StringSpec`, `WordSpec`.
- Unit-test pattern: `MockLoxoneClient` stubs `call()`, `callRaw()`, `callRawForData()`.
- Integration (JVM): `ktor-server-test-host` for WebSocket server simulation.
- **Acceptance tests** (`jvmAcceptanceTest`): require real miniserver.
  Set env: `LOX_ADDRESS`, `LOX_USER`, `LOX_PASS`. Excluded from CI.
- All tests run on JUnit Platform.

## Key frameworks

- **Ktor** Client (HTTP + WebSocket), **kotlinx.serialization** (JSON),
  **kotlinx.coroutines** (async), **kotlincrypto** (SHA/HMAC).

## Entrypoints

| Symbol | Role |
|---|---|
| `LoxoneClient` / `HttpLoxoneClient` / `WebsocketLoxoneClient` | Client abstractions |
| `KtorHttpLoxoneClient` / `KtorWebsocketLoxoneClient` | Concrete HTTP/WS clients |
| `LoxoneTokenAuthenticator` + `LoxoneProfile` + `LoxoneCredentials` | JWT auth flow |
| `LoxoneEndpoint` | Miniserver URL config |
| `LoxoneState` | Thread-safe state store — `collectFrom(events)` + `getValue(uuid)` |
| `LoxoneApp` | Structure file (rooms, categories, controls, sub-controls) |
| `Command<RESPONSE>` / `LoxoneMsgCommand` | Command pattern |

## Release

- `axion-release` plugin: version from Git tags `loxone-client-kotlin-X.Y.Z`.
- Triggered manually via GitHub Actions (`workflow_dispatch`) with version increment choice.
- Publishes signed artifacts to Maven Central via Sonatype.

## Commit convention

- Conventional commits required (`feat:`, `fix:`, `chore:`, `docs:`, `test:`, `refactor:`, `style:`, `perf:`, `ci:`, `build:`).
- Validated via commitlint in CI. The commit `"Initial plan"` is ignored.

## Known gaps (contributions welcome)

- Command encryption (AES + RSA)
- Control commands
- Token persistence and refresh
- CloudDNS discovery
- Icon/image download, statistics data
