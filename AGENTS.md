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
./gradlew kotlinUpgradeYarnLock     # regenerate kotlin-js-store/yarn.lock — run after any npm() change
```

## Build

- **JDK 21 required** for build; produces JVM 17 bytecode.
- Version catalog at `gradle/libs.versions.toml` — never hardcode versions.
- Native tests (`linuxX64Test`, `linuxArm64Test`) and JS browser tests are **disabled**.
- Ktor engine: CIO on JVM/Linux, JS engine on browser.
- **npm dependencies**: after adding, changing, or removing any `npm("pkg", "version")` call in
  `build.gradle.kts`, run `./gradlew kotlinUpgradeYarnLock` and commit the updated
  `kotlin-js-store/yarn.lock`. CI enforces this via `kotlinStoreYarnLock`.

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

- Follow https://www.conventionalcommits.org/en/v1.0.0/
- Format: `type(scope): description` — body lines ≤ 100 chars
- Allowed types: `feat`, `fix`, `chore`, `docs`, `test`, `refactor`, `style`, `perf`, `ci`, `build`
- Scopes: `core`, `auth`, `app`, `state`, `http`, `ws`, `build`, `ci`, `deps`, `docs`, `test`, `release`
  - e.g. `feat(core): add timeout parameter`, `fix(auth): handle token expiry`, `build(deps): pin ws to 8.20.1`
- Validated via commitlint in CI (`"Initial plan"` ignored).

## Known gaps (contributions welcome)

- Control commands
- Token persistence and refresh
- CloudDNS discovery
- Icon/image download, statistics data
