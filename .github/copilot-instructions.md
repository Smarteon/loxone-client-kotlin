# GitHub Copilot Instructions for loxone-client-kotlin

## Project Overview

This is a Kotlin Multiplatform library implementing the Loxone™ communication protocol. The library supports multiple targets including JVM, JavaScript (browser), and Linux native (x64, ARM64).

**Key Characteristics:**
- Experimental Kotlin multiplatform client for Loxone miniserver communication
- Uses Ktor client for HTTP and WebSocket communication
- Supports Loxone API protocol with token-based authentication
- Published to Maven Central

## Conventional Commits

**ALWAYS** follow the Conventional Commits specification for all commit messages.

**Format:** `<type>: <description>`

**Required types:**
- `feat:` - New features
- `fix:` - Bug fixes
- `chore:` - Maintenance tasks (dependencies, build config)
- `docs:` - Documentation changes
- `test:` - Test additions or modifications
- `refactor:` - Code refactoring without changing functionality
- `style:` - Code style changes (formatting, missing semi-colons, etc.)
- `perf:` - Performance improvements
- `ci:` - CI/CD pipeline changes
- `build:` - Build system changes

**Examples:**
- `feat: add support for miniserver gen 3`
- `fix: handle timeout on websocket connection`
- `chore: upgrade kotlin to latest version`
- `docs: update README with new examples`

The project uses `commitlint` to validate commit messages in pull requests.

## Build System

### Gradle with Kotlin DSL

The project uses Gradle with Kotlin DSL (`.gradle.kts` files) and modern Gradle features:

**Gradle Version Catalog:** Dependencies are managed via `gradle/libs.versions.toml`
- Do NOT hardcode version numbers in build files
- Add new dependencies to the version catalog first
- Use `libs.` prefix to reference catalog dependencies

**JVM Toolchain:** Project requires JDK 21 for builds but targets JVM 17 for runtime compatibility

### Common Build Commands

```bash
# Full build (excluding acceptance tests)
./gradlew clean build -x jvmAcceptanceTest

# Run JVM tests only
./gradlew jvmTest

# Run all verification tasks (tests + linting)
./gradlew check

# Run code linting with Detekt
./gradlew detekt

# Generate code coverage reports
./gradlew koverXmlReport

# Publish to local Maven repository
./gradlew publishToMavenLocal
```

### Multiplatform Build Targets

- **JVM** (`jvm`): Main target with full feature support
- **JavaScript** (`js`): Browser support with limited testing
- **Linux Native** (`linuxX64`, `linuxArm64`): Native Linux executables

**Source Set Structure:**
- `commonMain` - Shared code across all platforms
- `commonTest` - Shared test code
- `jvmMain` / `jvmTest` - JVM-specific code and tests
- `jvmAcceptanceTest` - Integration tests requiring real Loxone miniserver
- `jsMain` - JavaScript-specific code
- `linuxMain` - Linux native shared code
- `nativeMain` - All native targets shared code

## Testing Approach

### Testing Framework: Kotest

The project uses **Kotest** as the primary testing framework with its spec-based DSL.

**Preferred Test Style:** `ShouldSpec`
```kotlin
class MyTest : ShouldSpec({
    context("given some context") {
        should("test something") {
            // test code
        }
    }
})
```

**Common Test Patterns:**
- Use `should` for test cases, not `test` or other keywords
- Use `context` for grouping related tests
- Use Kotest matchers: `shouldBe`, `shouldNotBeNull`, `shouldBeGreaterThan`, etc.

### Test Categories

1. **Unit Tests** (`commonTest`, `jvmTest`)
   - Fast, isolated tests
   - Mock external dependencies using `ktor-client-mock`
   - Use `MockLoxoneClient` for testing client interactions

2. **Integration Tests** (`jvmTest`)
   - Test real network interactions using `ktor-server-test-host`
   - Test WebSocket communication with test servers

3. **Acceptance Tests** (`jvmAcceptanceTest`)
   - Require real Loxone miniserver connection
   - Set environment variables: `LOX_ADDRESS`, `LOX_USER`, `LOX_PASS`
   - Run with: `./gradlew jvmAcceptanceTest`
   - NOT run in regular CI builds

### Coroutines Testing

- Use `kotlinx-coroutines-test` for testing async code
- Tests are suspending functions by nature in Kotest

## Code Style and Linting

### Detekt Configuration

Static code analysis is enforced using **Detekt** with custom configuration.

**Configuration:** `detekt.yml` in project root

**Key Rules:**
- Package naming must start with `cz.smarteon.loxkt`
- Wildcard imports are allowed for `io.ktor.*` packages
- Formatting rules are applied via `detekt-formatting` plugin
- Build fails if any Detekt issues are found (`maxIssues: 0`)

**Always run Detekt before committing:**
```bash
./gradlew detekt
```

### Kotlin Code Conventions

- Use Kotlin idiomatic code style
- Prefer Kotlin stdlib functions over manual implementations
- Use Kotlin coroutines for async operations
- Use data classes for DTOs and messages
- Use sealed classes/interfaces for type hierarchies
- Prefer immutable data structures

### Serialization

- Use **kotlinx.serialization** for JSON serialization
- Annotate serializable classes with `@Serializable`
- Custom serializers should inherit from `KSerializer`
- Use `@SerialName` for JSON field mapping when needed

## Architecture Patterns

### Ktor Client Usage

All network communication uses Ktor client:
```kotlin
// Configure client with required features
HttpClient {
    install(ContentNegotiation) {
        json()
    }
    install(WebSockets)
    install(Logging)
}
```

### Client Interfaces

- `LoxoneClient` - Base interface for all clients
- `HttpLoxoneClient` - HTTP-based communication
- `WebsocketLoxoneClient` - WebSocket-based communication

### Authentication

Token-based authentication pattern:
- `LoxoneTokenAuthenticator` handles authentication flow
- `TokenRepository` stores and retrieves tokens
- Tokens are refreshed automatically before expiration

### Commands and Responses

- Commands extend `Command<RESPONSE>` interface
- Responses implement `LoxoneResponse` interface
- Use `LoxoneMsgCommand` for structured message-based commands

## Dependencies Management

### Core Dependencies

- **Kotlin Standard Library** - Core language features
- **Ktor Client** - HTTP and WebSocket communication
- **kotlinx.serialization** - JSON serialization
- **kotlinx.coroutines** - Async programming
- **kotlinx.datetime** - Date and time handling
- **kotlin-logging** - Logging facade
- **kotlincrypto** - Cryptographic operations (SHA, HMAC)

### Test Dependencies

- **Kotest** - Testing framework
- **ktor-client-mock** - Mocking HTTP client
- **ktor-server-test-host** - Testing server
- **slf4j-simple** - Logging implementation for tests

### Adding Dependencies

1. Add version to `[versions]` section in `gradle/libs.versions.toml`
2. Add library to `[libraries]` section
3. Reference in appropriate source set: `commonMain`, `jvmMain`, etc.
4. Prefer common dependencies over platform-specific when possible

## Release Process

The project uses **axion-release** plugin for version management:
- Versions are derived from Git tags
- Tags follow format: `loxone-client-kotlin-X.Y.Z`
- Release is triggered via GitHub Actions workflow
- Published artifacts go to Maven Central via Sonatype

## Documentation

- Use KDoc for public API documentation
- Follow Kotlin documentation conventions
- Keep README.md updated with usage examples
- Generate docs with: `./gradlew dokkaHtml`

## Example Code

Refer to `examples/kotlin` and `examples/java` directories for usage examples demonstrating:
- Client initialization
- Authentication
- Command execution
- WebSocket communication

## Platform-Specific Considerations

### JVM Target
- Full feature support
- Uses CIO engine for Ktor
- SLF4J for logging

### JavaScript Target
- Browser-only support
- Uses JS engine for Ktor
- Limited testing (browser tests disabled)

### Native Targets
- Linux x64 and ARM64
- Uses CIO engine for Ktor
- Limited to Linux platforms only

## Common Pitfalls

1. **Don't add dependencies without using version catalog**
2. **Don't skip Detekt checks** - Always run before committing
3. **Don't use blocking code** - Use coroutines for async operations
4. **Don't forget platform-specific source sets** - Code in `commonMain` must work on all platforms
5. **Don't use JVM-specific APIs in common code** - Use expect/actual pattern for platform-specific code
6. **Test on multiple targets** - Ensure changes work across JVM, JS, and native when modifying common code
