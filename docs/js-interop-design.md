# JavaScript / TypeScript Interop Design

This document records the decisions made when designing the Kotlin/JS export surface for this
library. The goal was to make the library usable from plain TypeScript projects while keeping
the core Kotlin types as the single source of truth — no redundant JS-only wrapper DTOs.

---

## Core principle: export Kotlin types directly

A previous approach mixed direct exports with a JS facade (`LoxoneJsClient`,
`LoxStateValue`, `LoxState`, `LoxControl`). This created a dual access path where the same
data existed in two parallel class hierarchies.

**Chosen direction**: annotate core Kotlin types with `@JsExport` where possible. The facade
types are dropped entirely. TypeScript consumers access `Control`, `LoxoneState`, `ValueState`,
etc. directly — the same types Kotlin consumers use.

---

## What is and is not exportable

The Kotlin/JS compiler maps Kotlin types to TypeScript automatically, but not all types are
supported in `@JsExport` declarations.

### Exportable (used as-is)
- Primitives: `Int`, `Double`, `Boolean`, `String`
- `Long` — exported as `bigint` with compiler flag `-Xes-long-as-bigint`
  (also requires `-XXLanguage:+JsAllowLongInExportedDeclarations`)
- `List<T>` → `KtList<T>` in TypeScript; consumers call `.asJsReadonlyArrayView()` for a
  native JS Array. Domain types also expose `xxxArray` computed properties (e.g.
  `DaytimerState.entriesArray`, `LoxoneApp.controlsArray`) that return `Array<T>` directly.
- `Map<K,V>` → `KtMap<K,V>` in TypeScript; consumers call `.asJsReadonlyMapView()` for a
  native ES2015 Map. Domain types expose `xxxArray` properties returning the map values as
  `Array<V>` (e.g. `LoxoneApp.roomsArray`, `LoxoneApp.controlsArray`).
- `Array<T>` — exported as a native JS/TS array directly

### Not exportable
- `UInt` — no unsigned integer in TypeScript. Fields that used `UInt` were changed to `Int`.
  Safe for Loxone timestamps (seconds since 2009) until approximately 2077.
- `JsonElement` (kotlinx.serialization) — not in Kotlin's core type table. Fields of this type
  are automatically excluded from the generated `.d.ts` by the compiler (with a warning).
  Affected: `Control.details` and `SubControl.details`.
- `SharedFlow<T>` (kotlinx.coroutines) — not exportable. The `events` flow on `LoxoneMiniserver`
  is exposed via JS callbacks instead (`onValueChanged`, `onTextChanged`).
- `suspend fun` — cannot be *declared* directly in an `@JsExport` class. Inherited suspend
  functions from a non-exported base class are callable at runtime but do not appear in the
  generated TypeScript declaration (see session design below).

---

## LoxoneState: copy-on-write instead of suspend

`LoxoneState` was originally a `suspend`-based class using a `Mutex` to protect concurrent
writes. Suspend is not compatible with `@JsExport` (accessors would need to return `Promise`).

**Decision**: replace `Mutex` + `suspend` with `@Volatile` copy-on-write semantics:

```kotlin
@Volatile private var states: Map<String, StateValue> = emptyMap()

internal fun update(event: LoxoneEvent) {
    states = states + (event.uuid to newState)
}
```

Trade-off: writes are no longer atomic with respect to other writes, but read consistency
is guaranteed (each read sees a complete snapshot). For a unidirectional data flow where only
the library writes and consumers read, this is safe.

`getAllUuids()` returns `Array<String>` rather than `Set<String>` because `KtSet` is less
ergonomic in TypeScript than a plain array. This is a breaking change from the previous API.

---

## Session class: commonMain base + jsMain extension

The `LoxoneMiniserver` class (wiring up WebSocket client, HTTP client, authentication, and
live state) lives in `commonMain` so JVM and native targets benefit too — not just JS. But
`SharedFlow<LoxoneEvent>` is not JS-exportable, and `@JsExport` classes cannot declare
`suspend fun` members. Annotating the base class directly fails for the same reason.

### Two-class hierarchy

```
commonMain  LoxoneMiniserver       open class, suspend API, SharedFlow events
jsMain      JsLoxoneMiniserver     @JsExport @JsName("LoxoneMiniserver"), extends LoxoneMiniserver
```

- JVM/native consumers use `LoxoneMiniserver` directly with `events.collect { }`.
- TypeScript consumers use `JsLoxoneMiniserver`, which appears as `LoxoneMiniserver` in the
  generated `.d.mts`. Suspend methods are exposed as explicit Promise-returning wrappers.
  Live events are subscribed via `onValueChanged` / `onTextChanged` callbacks.

### Workaround: explicit wrappers with `@JsName`

Kotlin/JS does not include inherited members of non-exported base classes in the generated
TypeScript declarations. Each inherited member that needs to appear in the TypeScript declaration
is redeclared in `JsLoxoneMiniserver` under a unique Kotlin name, then renamed to the idiomatic
JS name via `@JsName`:

```kotlin
// Kotlin name avoids conflict with inherited suspend connect(); @JsName sets the JS name
@JsName("connect")
fun connectAsync(): Promise<Unit> = sessionScope.promise { super.connect() }

// Kotlin can't shadow an inherited open val with override inside @JsExport, so a new
// property name is used; @JsName makes it appear as `state` in TypeScript
@JsName("state")
val jsState: LoxoneState get() = super.state
```

The inherited `suspend fun connect()` compiles to a continuation-passing JS function
(`connect($completion)`), so the `@JsName("connect")` wrapper (`connect()`) does not collide
with it in the JavaScript prototype chain.

Result in the generated `.d.mts`:
```typescript
export declare class LoxoneMiniserver {
    constructor(endpoint: LoxoneEndpoint, user: string, password: string);
    constructor(url: string, user: string, password: string);
    get state(): LoxoneState;
    connect(): Promise<void>;
    loadStructure(): Promise<LoxoneApp>;
    close(): Promise<void>;
    onValueChanged(cb: (uuid: string, value: number) => void): () => void;
    onTextChanged(cb: (uuid: string, text: string) => void): () => void;
}
```

---

## Build configuration

`useEsModules()` + `binaries.library()` produces an ES module bundle. The TypeScript
definitions are emitted as `.d.mts` alongside the `.mjs` bundle in
`build/dist/js/productionLibrary/`.

```kotlin
// build.gradle.kts
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.js.ExperimentalJsExport",
            "-Xes-long-as-bigint",
            "-XXLanguage:+JsAllowLongInExportedDeclarations"
        )
    }
    js {
        browser { testTask { enabled = false } }
        useEsModules()
        binaries.library()
        generateTypeScriptDefinitions()
    }
}
```

---

## Crypto module resolution (ESM)

The `jsMain` crypto `actual`s depend on two npm packages. Because the build emits an ES module
(`useEsModules()`), `require(...)` is **undefined at runtime** — in browsers there is no `require`
at all, and Node loads the `.mjs` as ESM. The npm modules must therefore be declared as
`@JsModule` externals so the compiler emits `import` statements:

```kotlin
@JsModule("node-forge")
internal external val forge: dynamic

@JsModule("crypto-js")
internal external val cryptoJs: dynamic
```

- **AES** uses `crypto-js` (`4.2.0`) — its CJS/UMD default export carries the whole API and
  resolves cleanly under both Node ESM and browser bundlers.
- **RSA** uses `node-forge` (`1.3.1`, `RSAES-PKCS1-V1_5`). It replaced `jsencrypt`, whose npm
  `main` is a browser-only webpack bundle that references `window` and throws
  `ReferenceError: window is not defined` when loaded under Node. `node-forge` has a proper CJS
  entrypoint that loads in Node and is also bundler-friendly.

Regression guard: `src/jsTest/kotlin/CryptoJsTest.kt` round-trips both (crypto-js AES, and a
full `createSessionKey` → node-forge private-key decrypt) on the Node target.

## JS testing (Kotlin/JS + Kotest)

The `js` target defines a `nodejs()` test task (`jsNodeTest`). The shared `commonTest` specs use
Kotest **nested** containers (`context { should { } }`, WordSpec `When { should { } }`), which the
Kotlin/JS test runner does not support — nested registration throws
`IllegalStateException: Nested tests are not supported`. The JS run is therefore filtered to
flat, JS-only specs:

```kotlin
js {
    nodejs {
        testTask {
            useMocha { timeout = "30s" } // mocha's 2s default is too low for async specs
            filter.includeTestsMatching("*JsTest*")
        }
    }
    browser { testTask { enabled = false } }
}
```

JS-only specs live in `src/jsTest` with a `…JsTest` class-name suffix and use a single-level
style (`StringSpec`/`FunSpec`). They cover the JS-specific `actual`s (crypto interop) that
`commonTest` cannot. Platform-agnostic logic remains covered by `commonTest` on the JVM.

---

## Known limitations

- `Control.details` and `SubControl.details` (`JsonElement?`) are typed as `any` in the
  generated TypeScript. There is no lossless mapping for arbitrary JSON in the Kotlin type
  system without wrapping, which would contradict the direct-export principle.
- `SharedFlow<LoxoneEvent>` is not accessible from TypeScript. Raw event access requires
  either using the JVM API or subscribing through the callback API.
- Sending commands from TypeScript is not yet supported (depends on stable AES encryption
  being plumbed through to an exported API surface).
