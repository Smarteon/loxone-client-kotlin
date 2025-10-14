# GitHub Issues - Loxone Protocol Implementation

This document contains ready-to-use GitHub issue templates for implementing missing Loxone protocol features. Each issue corresponds to items identified in the protocol gap analysis.

Copy and paste these directly into GitHub Issues.

---

## Phase 1: Core Event Processing

### Issue 1: Implement Binary Event-Table Parsing

**Labels:** `enhancement`, `protocol`, `priority:high`, `multiplatform`

```markdown
## Description
Implement parsing for binary event tables that the Miniserver sends over WebSocket. Event tables contain real-time state updates for controls (values, text, daytimer, weather).

## Protocol Reference
- Document: `docs/loxone/CommunicatingWithMiniserver.md`
- Section: "Understanding the messages" → "Event-Tables"
- Protocol Version: 16.0

## Current Status
- MessageKind enum exists with EVENT_VALUE, EVENT_TEXT, EVENT_DAYTIMER, EVENT_WEATHER
- Message headers are parsed but binary event payloads are not processed
- TODO comment exists in `KtorWebsocketLoxoneClient.kt:129`

## Implementation Tasks
- [ ] Create event data classes (ValueEvent, TextEvent, DaytimerEvent, WeatherEvent)
- [ ] Implement binary parser for Value-State events (UUID + double, 24 bytes)
- [ ] Implement binary parser for Text-State events (UUID + UUID-Icon + length + text + padding)
- [ ] Implement binary parser for Daytimer-State events (UUID + default + nEntries + entries)
- [ ] Implement binary parser for Weather-State events (lastUpdate + nEntries + entries)
- [ ] Add event callback/flow mechanism to WebSocket client
- [ ] Handle multiple events in single message
- [ ] Write unit tests with known binary sequences
- [ ] Write integration tests
- [ ] Update documentation
- [ ] Add usage example

## Binary Formats

**Value-State Event (24 bytes):**
```c
typedef struct {
  PUUID uuid;      // 128-bit UUID
  double dVal;     // 64-bit Float (little endian)
} PACKED EvData;
```

**Text-State Event (variable):**
```c
typedef struct {
  PUUID uuid;          // 128-bit UUID
  PUUID uuidIcon;      // 128-bit UUID for icon
  unsigned int textLength;  // 32-bit unsigned int (little endian)
  char text[textLength];    // UTF-8 text
  // padding to multiple of 4 bytes
} PACKED;
```

**Daytimer-State Event (variable):**
```c
typedef struct {
  PUUID uuid;
  double defValue;
  unsigned int nEntries;
  // array of entries
} PACKED;
```

**Weather-State Event (variable):**
```c
typedef struct {
  unsigned int lastUpdate;  // seconds since 2009 UTC
  unsigned int nEntries;
  // array of entries
} PACKED;
```

## Technical Details
- All integers are little-endian
- UUIDs are 128-bit (16 bytes)
- Text must be UTF-8 decoded
- Padding bytes should be ignored
- One message can contain multiple events

## Testing
- Create binary test data with known UUIDs and values
- Verify parsing with multiple events in one message
- Test edge cases (empty text, no entries, etc.)
- Test padding handling for text events

## Dependencies
None

## Acceptance Criteria
- [ ] Can parse all four event types from binary data
- [ ] Events can be consumed via callback or Flow API
- [ ] UUID mapping works correctly
- [ ] All tests pass on JVM, JS, and Native targets
```

---

### Issue 2: Implement Enable Binary Status Updates Command

**Labels:** `enhancement`, `protocol`, `priority:high`

```markdown
## Description
Implement the `jdev/sps/enablebinstatusupdate` command that must be sent after authentication to start receiving binary event tables from the Miniserver.

## Protocol Reference
- Document: `docs/loxone/CommunicatingWithMiniserver.md`
- Section: "Handling an active connection"
- Protocol Version: 16.0

## Current Status
Command is not implemented. WebSocket client establishes connection but doesn't enable binary updates.

## Implementation Tasks
- [ ] Add command to LoxoneCommands
- [ ] Create response model (LoxoneMsg expected)
- [ ] Integrate into WebSocket client initialization flow
- [ ] Call automatically after authentication
- [ ] Handle command failure gracefully
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update WebSocket client documentation

## Technical Details
- Command: `jdev/sps/enablebinstatusupdate`
- Must be sent after successful authentication
- Response is standard LoxoneMsg
- After this command, Miniserver starts sending binary event tables

## Testing
- Mock WebSocket server that expects this command
- Verify command is sent in correct sequence (after auth)
- Test with acceptance tests against real Miniserver

## Dependencies
None (but events won't be received until Issue #1 is also completed)

## Acceptance Criteria
- [ ] Command is defined in LoxoneCommands
- [ ] Automatically called after authentication in WebSocket client
- [ ] Error handling for command failure
- [ ] All tests pass
```

---

### Issue 3: Complete Binary File Download Support

**Labels:** `enhancement`, `protocol`, `priority:high`, `multiplatform`

```markdown
## Description
Complete the implementation of `callRawForData` in WebSocket client to download binary files (images, statistics data) from the Miniserver.

## Protocol Reference
- Document: `docs/loxone/CommunicatingWithMiniserver.md`
- Section: "Understanding the messages" → "Binary Files"
- Protocol Version: 16.0

## Current Status
- `callRawForData` interface exists but returns TODO in WebSocket client
- HTTP client may have implementation
- MessageKind.FILE is defined but not handled

## Implementation Tasks
- [ ] Implement callRawForData in KtorWebsocketLoxoneClient
- [ ] Handle MessageKind.FILE binary message reception
- [ ] Implement binary data buffering
- [ ] Handle large file downloads
- [ ] Add timeout configuration for large downloads
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update documentation
- [ ] Add usage examples

## Technical Details
- Binary files are sent with MessageKind.FILE (1) header
- Message header contains file size
- File data follows header as raw bytes
- May need to handle files larger than memory

## Testing
- Test with mock small binary file
- Test with larger files
- Test timeout handling
- Verify across all platforms

## Dependencies
None

## Acceptance Criteria
- [ ] callRawForData works in WebSocket client
- [ ] Binary files are correctly downloaded
- [ ] Large files are handled efficiently
- [ ] All tests pass on all platforms
```

---

## Phase 2: Structure File & Control Commands

### Issue 4: Implement Structure File Download and Caching

**Labels:** `enhancement`, `protocol`, `priority:high`

```markdown
## Description
Implement downloading and caching of the LoxAPP3.json structure file, which contains the Miniserver's configuration, controls, rooms, categories, and UUID mappings.

## Protocol Reference
- Document: `docs/loxone/CommunicatingWithMiniserver.md`
- Section: "Structure-File: LoxAPP3.json"
- Protocol Version: 16.0

## Current Status
Commands are not implemented. No structure file support exists.

## Implementation Tasks
- [ ] Add `data/LoxAPP3.json` download command
- [ ] Add `jdev/sps/LoxAPPversion3` version check command
- [ ] Create StructureFileInfo model with lastModified
- [ ] Implement cache mechanism (memory + optional disk)
- [ ] Add version comparison logic
- [ ] Implement conditional download (only if newer)
- [ ] Parse basic structure file format (defer full control parsing)
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update documentation
- [ ] Add usage examples

## Technical Details
- Structure file is delivered as text message (JSON)
- Contains `lastModified` timestamp field
- Should check version before full download
- File can be large (several MB)
- Full structure file parsing can be separate issue

## API Design
```kotlin
interface StructureFileRepository {
    suspend fun getStructureFile(forceDownload: Boolean = false): StructureFile
    suspend fun checkVersion(): String
    suspend fun clearCache()
}
```

## Testing
- Mock structure file response
- Test version comparison
- Test cache hit/miss scenarios
- Test with real Miniserver in acceptance tests

## Dependencies
- Issue #3 (if downloading as binary, though it's usually text)

## Acceptance Criteria
- [ ] Can download structure file
- [ ] Version checking works
- [ ] Caching prevents unnecessary downloads
- [ ] Basic structure file model exists
- [ ] All tests pass
```

---

### Issue 5: Implement Control Commands

**Labels:** `enhancement`, `protocol`, `priority:high`

```markdown
## Description
Implement generic control commands to manipulate devices (switches, dimmers, blinds, etc.) via `jdev/sps/io/{uuid}/{command}`.

## Protocol Reference
- Document: `docs/loxone/CommunicatingWithMiniserver.md`
- Section: "General Info" → "Commands"
- Protocol Version: 16.0

## Current Status
No control command support exists. Only basic info commands are implemented.

## Implementation Tasks
- [ ] Add generic control command builder
- [ ] Create ControlCommand class with UUID and command parameters
- [ ] Handle response parsing (state after execution)
- [ ] Add typed builders for common controls (On/Off, Pulse, etc.)
- [ ] Support additional command parameters (e.g., dimmer value)
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update documentation
- [ ] Add usage examples for different control types

## Technical Details
- Command format: `jdev/sps/io/{uuid}/{command}`
- UUID is the control's unique identifier from structure file
- Common commands: On, Off, Pulse, Up, Down, UpDown, etc.
- Some commands take additional parameters
- Response is LoxoneMsg with updated state

## API Design
```kotlin
object LoxoneCommands {
    object Control {
        fun execute(uuid: String, command: String): LoxoneMsgCommand<ControlState>
        fun switchOn(uuid: String): LoxoneMsgCommand<ControlState>
        fun switchOff(uuid: String): LoxoneMsgCommand<ControlState>
        fun pulse(uuid: String): LoxoneMsgCommand<ControlState>
        fun setValue(uuid: String, value: Double): LoxoneMsgCommand<ControlState>
    }
}
```

## Testing
- Mock control commands and responses
- Test various control types
- Test command with parameters
- Test error handling (invalid UUID, etc.)
- Test with real controls in acceptance tests

## Dependencies
- Issue #4 (structure file for UUID discovery)

## Acceptance Criteria
- [ ] Generic control command works
- [ ] Typed builders for common commands exist
- [ ] Response parsing works
- [ ] All tests pass
- [ ] Documentation includes examples
```

---

## Phase 3: Command Encryption

### Issue 6: Implement RSA Public Key Retrieval and Management

**Labels:** `enhancement`, `protocol`, `priority:high`, `multiplatform`, `security`

```markdown
## Description
Implement retrieval and management of the Miniserver's RSA public key, which is used for encrypting AES session keys in command encryption.

## Protocol Reference
- Document: `docs/loxone/CommunicatingWithMiniserver.md`
- Section: "Command Encryption"
- Protocol Version: 16.0

## Current Status
No encryption support exists. LoxoneCrypto only handles hashing.

## Implementation Tasks
- [ ] Add `jdev/sys/getPublicKey` command
- [ ] Evaluate RSA crypto library for multiplatform support
- [ ] Add RSA public key model
- [ ] Implement X.509 DER/PEM parsing for public key
- [ ] Add public key storage/caching
- [ ] Implement RSA encryption (PKCS1, Base64 NoWrap)
- [ ] Write unit tests with known keys
- [ ] Write integration tests
- [ ] Update documentation

## Technical Details
- Command: `jdev/sys/getPublicKey`
- Returns X.509 encoded key in PEM format
- RSA encryption: ECB mode, PKCS1 padding
- Output: Base64 encoded, no line wrapping
- Multiplatform consideration: May need platform-specific implementations

## Crypto Library Options
- kotlincrypto/core (currently used for hashing)
- Platform-specific: JVM crypto, WebCrypto API, OpenSSL native
- Evaluate each for RSA support

## Testing
- Test with known RSA key pair
- Test encryption/decryption roundtrip
- Test Base64 encoding format
- Test on all platforms

## Dependencies
None

## Acceptance Criteria
- [ ] Public key can be retrieved from Miniserver
- [ ] RSA encryption works
- [ ] Key is cached and reused
- [ ] Works on JVM, JS, and Native
- [ ] All tests pass
```

---

### Issue 7: Implement AES Command Encryption

**Labels:** `enhancement`, `protocol`, `priority:high`, `multiplatform`, `security`

```markdown
## Description
Implement AES-256-CBC encryption for commands, allowing commands to be sent securely to the Miniserver via `jdev/sys/enc` and `jdev/sys/fenc`.

## Protocol Reference
- Document: `docs/loxone/CommunicatingWithMiniserver.md`
- Section: "Command Encryption" → "Step-by-step Guide HTTP Requests"
- Protocol Version: 16.0

## Current Status
No encryption support exists.

## Implementation Tasks
- [ ] Add AES-256-CBC encryption support
- [ ] Implement ZeroBytePadding
- [ ] Add random IV generation (16 bytes)
- [ ] Add random salt generation
- [ ] Implement command wrapping: `jdev/sys/enc/{cipher}`
- [ ] Implement command with encrypted response: `jdev/sys/fenc/{cipher}`
- [ ] Add URI component encoding for cipher text
- [ ] Handle response decryption for fenc
- [ ] Write unit tests with known keys/IVs
- [ ] Write integration tests
- [ ] Update documentation

## Technical Details
- AES-256-CBC with ZeroBytePadding
- 16-byte IV, 32-byte key, 16-byte block size
- Base64 encoding with no line wrapping
- Salt prepended: `salt/{salt}/{command}`
- For HTTP: Session key sent via RSA encryption
- For WebSocket: Use negotiated session key

## Encryption Flow (HTTP)
1. Generate random salt (2+ bytes hex)
2. Generate AES key (32 bytes hex)
3. Generate AES IV (16 bytes hex)
4. Prepend salt to command
5. AES encrypt with key + IV
6. Base64 encode
7. URI encode
8. RSA encrypt key:IV pair
9. Append to command

## API Design
```kotlin
interface CommandEncryption {
    suspend fun encryptCommand(command: String): String
    suspend fun encryptCommandWithResponse(command: String): Pair<String, suspend (String) -> String>
}
```

## Testing
- Test with known AES keys and IVs
- Test encryption/decryption roundtrip
- Test salt prepending
- Test Base64 and URI encoding
- Test on all platforms

## Dependencies
- Issue #6 (RSA for session key encryption)

## Acceptance Criteria
- [ ] AES encryption works correctly
- [ ] enc and fenc commands work
- [ ] Response decryption works for fenc
- [ ] Works on all platforms
- [ ] All tests pass
```

---

### Issue 8: Implement WebSocket Key Exchange

**Labels:** `enhancement`, `protocol`, `priority:high`, `security`

```markdown
## Description
Implement the key exchange process for WebSocket connections, where AES session keys are negotiated during connection establishment using RSA encryption.

## Protocol Reference
- Document: `docs/loxone/CommunicatingWithMiniserver.md`
- Section: "Setting up a connection" → "Step-by-step guide"
- Protocol Version: 16.0

## Current Status
WebSocket connection established but no key exchange performed.

## Implementation Tasks
- [ ] Add `jdev/sys/keyexchange/{encrypted-session-key}` command
- [ ] Generate AES key and IV during WebSocket setup
- [ ] RSA encrypt key:IV pair
- [ ] Send keyexchange command after WebSocket open
- [ ] Store negotiated key/IV/salt for session
- [ ] Use negotiated keys for command encryption
- [ ] Integrate with WebSocket client initialization
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update documentation

## Technical Details
- Generate AES-256 key (32 bytes hex)
- Generate AES IV (16 bytes hex)
- Generate random salt (hex string)
- RSA encrypt "{key}:{iv}" with public key
- Send encrypted session key via keyexchange command
- Use these keys for all WebSocket command encryption
- Command: `jdev/sys/keyexchange/{encrypted-session-key}`

## WebSocket Encryption Flow
1. After WebSocket open, before authentication
2. Generate AES key, IV, salt
3. RSA encrypt key:IV
4. Send keyexchange command
5. Use negotiated keys for subsequent encrypted commands

## Testing
- Mock WebSocket server expecting key exchange
- Test key generation and encryption
- Test command encryption with negotiated keys
- Test with real Miniserver

## Dependencies
- Issue #6 (RSA public key)
- Issue #7 (AES encryption)

## Acceptance Criteria
- [ ] Key exchange works in WebSocket client
- [ ] Session keys are generated and stored
- [ ] Commands can be encrypted using session keys
- [ ] All tests pass
```

---

## Phase 4: Certificate & TLS Support

### Issue 9: Implement Certificate Retrieval and Verification

**Labels:** `enhancement`, `protocol`, `priority:medium`, `multiplatform`, `security`

```markdown
## Description
Implement certificate retrieval and verification to ensure secure connections to the Miniserver, including Loxone Root Certificate validation.

## Protocol Reference
- Document: `docs/loxone/CommunicatingWithMiniserver.md`
- Section: "Setting up a connection" → "Step-by-step guide"
- Protocol Version: 16.0

## Current Status
No certificate handling exists. Ktor default TLS validation is used.

## Implementation Tasks
- [ ] Add `jdev/sys/getcertificate` command
- [ ] Implement certificate chain parsing
- [ ] Add Loxone Root Certificate validation
- [ ] Extract public key from certificate
- [ ] Implement certificate storage
- [ ] Add certificate pinning option
- [ ] Handle expired certificates (httpsStatus = 2)
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update documentation

## Technical Details
- Command: `jdev/sys/getcertificate`
- Returns certificate chain
- Root certificate must be verified against Loxone Root Certificate
- Public key extracted from last certificate
- X.509 format
- Multiplatform consideration: Platform-specific X.509 parsing

## Certificate Verification Steps
1. Download certificate chain
2. Parse certificate chain
3. Verify root is Loxone Root Certificate
4. Verify chain validity
5. Extract public key from last certificate
6. Store for RSA encryption

## Testing
- Test with known certificate chain
- Test with self-signed certificate
- Test chain verification
- Test on all platforms

## Dependencies
- Issue #6 (uses same public key extraction logic)

## Acceptance Criteria
- [ ] Certificate can be downloaded
- [ ] Chain verification works
- [ ] Public key extraction works
- [ ] Works on all platforms
- [ ] All tests pass
```

---

## Phase 5: Token Management Extensions

### Issue 10: Implement Token Refresh Command

**Labels:** `enhancement`, `protocol`, `priority:medium`

```markdown
## Description
Implement token refresh functionality to extend token lifespan without re-authentication using credentials.

## Protocol Reference
- Document: `docs/loxone/CommunicatingWithMiniserver.md`
- Section: "Tokens" → "Refreshing tokens"
- Protocol Version: 16.0

## Current Status
- Token expiry detection exists (TokenState)
- Refresh command not implemented
- No automatic refresh

## Implementation Tasks
- [ ] Add `jdev/sys/refreshjwt/{tokenHash}/{user}` command
- [ ] Support both hashed and plaintext token (11.2+)
- [ ] Integrate with TokenAuthenticator for automatic refresh
- [ ] Update Token model with new validUntil and token
- [ ] Add refresh threshold configuration
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update documentation

## Technical Details
- Command: `jdev/sys/refreshjwt/{tokenHash}/{user}`
- tokenHash: HMAC hash of token using getkey2 result
- Since 11.2: Can also send token in plaintext
- Response contains new token, validUntil, unsecurePass
- Only works if token still valid
- Recommended to refresh before expiry

## Refresh Strategy
- Check expiry in TokenState
- Refresh when < 5 minutes remaining (configurable)
- Automatic refresh in background
- Handle refresh failures gracefully

## Testing
- Mock refresh command responses
- Test automatic refresh trigger
- Test refresh failure handling
- Test with real Miniserver

## Dependencies
None

## Acceptance Criteria
- [ ] Refresh command works
- [ ] Automatic refresh integrated in TokenAuthenticator
- [ ] Both hashed and plaintext token supported
- [ ] All tests pass
```

---

### Issue 11: Implement Token Validity Check

**Labels:** `enhancement`, `protocol`, `priority:medium`

```markdown
## Description
Implement command to check if a token is still valid without refreshing it.

## Protocol Reference
- Document: `docs/loxone/CommunicatingWithMiniserver.md`
- Section: "Tokens" → "Checking if tokens are valid"
- Protocol Version: 16.0

## Current Status
No validity check command exists. Only local expiry check available.

## Implementation Tasks
- [ ] Add `jdev/sys/checktoken/{tokenHash}/{user}` command
- [ ] Create TokenValidityResult model
- [ ] Add convenience method in TokenAuthenticator
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update documentation

## Technical Details
- Command: `jdev/sys/checktoken/{tokenHash}/{user}`
- Returns token validity status
- Does not refresh/extend token
- Can be used before attempting to use token
- Available since firmware 10.0

## Use Cases
- Check token before starting session
- Validate stored token on app startup
- Diagnostic/debugging

## Testing
- Mock valid token response
- Mock invalid token response
- Test with real Miniserver

## Dependencies
None

## Acceptance Criteria
- [ ] Check command works
- [ ] Returns correct validity status
- [ ] Integrated with TokenAuthenticator
- [ ] All tests pass
```

---

### Issue 12: Implement Token Persistence

**Labels:** `enhancement`, `protocol`, `priority:medium`, `multiplatform`

```markdown
## Description
Implement token persistence to save and load tokens across application sessions, avoiding repeated authentication.

## Protocol Reference
- Document: `docs/loxone/CommunicatingWithMiniserver.md`
- Section: "Tokens"
- Protocol Version: 16.0

## Current Status
- TokenRepository interface exists but no implementations
- Tokens not persisted across sessions

## Implementation Tasks
- [ ] Create InMemoryTokenRepository (already exists?)
- [ ] Create FileTokenRepository for JVM
- [ ] Create BrowserStorageTokenRepository for JS
- [ ] Create NativeTokenRepository for Native
- [ ] Add encryption for stored tokens
- [ ] Add token cleanup on kill
- [ ] Add migration from old storage format
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update documentation

## Technical Details
- Tokens should be encrypted when stored
- Store per user and endpoint
- Handle multiple tokens for different miniservers
- Clean up expired/killed tokens
- Platform-specific storage mechanisms

## Storage Mechanisms
- JVM: Encrypted file in user home or app data dir
- JS: LocalStorage or IndexedDB (encrypted)
- Native: Platform-specific secure storage

## API Design
```kotlin
interface TokenRepository {
    suspend fun saveToken(endpoint: LoxoneEndpoint, user: String, token: Token)
    suspend fun loadToken(endpoint: LoxoneEndpoint, user: String): Token?
    suspend fun deleteToken(endpoint: LoxoneEndpoint, user: String)
    suspend fun clearAll()
}
```

## Testing
- Test save/load roundtrip
- Test encryption
- Test multiple tokens
- Test cleanup
- Test on all platforms

## Dependencies
None (but benefits from Issue #10, #11 for token management)

## Acceptance Criteria
- [ ] Tokens can be persisted
- [ ] Tokens are encrypted
- [ ] Works on all platforms
- [ ] Token cleanup works
- [ ] All tests pass
```

---

## Phase 6: CloudDNS & Discovery

### Issue 13: Implement CloudDNS Client

**Labels:** `enhancement`, `protocol`, `priority:medium`

```markdown
## Description
Implement CloudDNS client to discover Miniserver IP address and port using the Loxone CloudDNS service.

## Protocol Reference
- Document: `docs/loxone/CommunicatingWithMiniserver.md`
- Section: "General Info" → "CloudDNS"
- Protocol Version: 16.0

## Current Status
No CloudDNS support exists.

## Implementation Tasks
- [ ] Create CloudDNS client
- [ ] Add DNS lookup: `dns.loxonecloud.com/?getip&snr={SNR}&json=true`
- [ ] Create CloudDnsResponse model
- [ ] Parse all response fields (Code, IP, PortOpen, LastUpdated, IPHTTPS, etc.)
- [ ] Handle all error codes (403, 405, 409, 412, 418, 481, 482, 483)
- [ ] Detect HTTPS/WSS support
- [ ] Add timeout and retry logic
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update documentation

## Technical Details
- HTTP GET request to CloudDNS service
- Returns JSON with Miniserver info
- Response codes indicate various states
- IPHTTPS and PortOpenHTTPS for Gen 2+ Miniservers
- LastUpdated shows when Miniserver last reported

## Response Codes
- 200: OK
- 403: CloudDNS disabled
- 405: Miniserver not reporting
- 409: Insecure password, external access blocked
- 412: Port not opened
- 418: Denied (see responseJSON)
- 481: Could not connect to remote connect
- 482: Remote connect timeout
- 483: Scheduled restart

## API Design
```kotlin
data class CloudDnsResponse(
    val code: Int,
    val ip: String?,
    val port: Int?,
    val portOpen: Boolean?,
    val lastUpdated: String?,
    val ipHttps: String?,
    val portHttps: Int?,
    val portOpenHttps: Boolean?
)

interface CloudDnsClient {
    suspend fun lookup(serialNumber: String): CloudDnsResponse
}
```

## Testing
- Mock CloudDNS responses
- Test all response codes
- Test HTTPS detection
- Test error handling

## Dependencies
None

## Acceptance Criteria
- [ ] CloudDNS lookup works
- [ ] All response codes handled
- [ ] HTTPS/WSS detection works
- [ ] All tests pass
```

---

### Issue 14: Implement Automatic Endpoint Resolution

**Labels:** `enhancement`, `protocol`, `priority:medium`

```markdown
## Description
Implement factory methods to automatically create LoxoneEndpoint from CloudDNS lookup, including automatic HTTP/HTTPS protocol selection.

## Protocol Reference
- Document: `docs/loxone/CommunicatingWithMiniserver.md`
- Sections: "CloudDNS", "Using HTTPS/WSS", "Remote Connect Service"
- Protocol Version: 16.0

## Current Status
LoxoneEndpoint must be created manually with known host/port.

## Implementation Tasks
- [ ] Add LoxoneEndpoint.fromCloudDns(snr: String) factory
- [ ] Implement automatic protocol selection based on httpsStatus
- [ ] Add local vs remote detection
- [ ] Add Remote Connect Service support
- [ ] Implement fallback strategy (HTTPS -> HTTP)
- [ ] Add connection testing
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update documentation
- [ ] Add usage examples

## Technical Details
- Use CloudDNS to get IP/port
- Check httpsStatus for TLS support:
  - 0: No HTTPS support
  - 1: HTTPS supported
  - 2: Certificate expired
- Prefer HTTPS when available
- Handle Remote Connect Service
- Test local connectivity first

## Resolution Strategy
1. Lookup via CloudDNS
2. Check httpsStatus
3. If HTTPS supported, try HTTPS first
4. If HTTPS fails or not supported, try HTTP
5. If Remote Connect available, use it
6. Store resolved endpoint for reuse

## API Design
```kotlin
object LoxoneEndpoint {
    suspend fun fromCloudDns(
        serialNumber: String,
        preferHttps: Boolean = true,
        testConnection: Boolean = true
    ): LoxoneEndpoint
    
    suspend fun fromCloudDnsWithFallback(
        serialNumber: String
    ): LoxoneEndpoint
}
```

## Testing
- Mock CloudDNS responses
- Test HTTPS selection
- Test fallback to HTTP
- Test connection testing
- Test with real Miniserver

## Dependencies
- Issue #13 (CloudDNS client)
- Issue #9 (certificate handling)

## Acceptance Criteria
- [ ] Automatic endpoint creation works
- [ ] Protocol selection correct
- [ ] Fallback works
- [ ] All tests pass
```

---

## Phase 7: Enhanced Error Handling

### Issue 15: Implement WebSocket Close Code Handling

**Labels:** `enhancement`, `protocol`, `priority:low`, `good-first-issue`

```markdown
## Description
Implement interpretation and handling of custom WebSocket close codes that the Miniserver sends to indicate various error conditions.

## Protocol Reference
- Document: `docs/loxone/CommunicatingWithMiniserver.md`
- Section: "Websocket Close Codes"
- Protocol Version: 16.0

## Current Status
Basic close handling exists but custom codes not interpreted.

## Implementation Tasks
- [ ] Create LoxoneCloseCode enum with all codes
- [ ] Add close code interpretation
- [ ] Create user-friendly error messages
- [ ] Add retry logic for specific codes
- [ ] Handle close events in WebSocket client
- [ ] Add callback for close events
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update documentation

## Close Codes
- 4003: Too many failed login attempts / blocked
- 4004: Some user has been changed
- 4005: Current user has been changed
- 4006: User has been disabled
- 4007: Miniserver performing update
- 4008: No event slots available

## Handling Strategy
- 4003: Wait before retry, inform user
- 4004: Reconnect may be safe
- 4005: Need to re-authenticate
- 4006: Cannot reconnect, inform user
- 4007: Retry after delay
- 4008: Cannot receive events, inform user

## API Design
```kotlin
enum class LoxoneCloseCode(val code: Int, val message: String) {
    BLOCKED(4003, "Too many failed login attempts"),
    USER_CHANGED(4004, "A user has been changed"),
    CURRENT_USER_CHANGED(4005, "Current user has been changed"),
    USER_DISABLED(4006, "User has been disabled"),
    UPDATE_IN_PROGRESS(4007, "Miniserver is updating"),
    NO_EVENT_SLOTS(4008, "No event slots available")
}

interface CloseHandler {
    suspend fun onClose(code: LoxoneCloseCode)
}
```

## Testing
- Mock close events with various codes
- Test error messages
- Test retry logic
- Test callback invocation

## Dependencies
None

## Acceptance Criteria
- [ ] All close codes interpreted
- [ ] User-friendly messages exist
- [ ] Retry logic works
- [ ] All tests pass
```

---

### Issue 16: Extend Error Code Coverage

**Labels:** `enhancement`, `protocol`, `priority:low`, `good-first-issue`

```markdown
## Description
Add support for additional HTTP error codes returned by the Miniserver and create meaningful exceptions for each.

## Protocol Reference
- Document: `docs/loxone/CommunicatingWithMiniserver.md`
- Section: "Error-Codes" → "Returned Error-Codes"
- Protocol Version: 16.0

## Current Status
Some error codes defined in LoxoneMsg (200, 400, 401, 404, 420, 500).

## Implementation Tasks
- [ ] Add missing error code constants to LoxoneMsg
- [ ] Create specific exception classes for each code
- [ ] Add error code to exception mapping
- [ ] Update error handling in clients
- [ ] Add user-friendly error messages
- [ ] Write unit tests
- [ ] Update documentation

## Missing Error Codes
- 403: Insufficient rights / Encryption failed
- 423: User disabled
- 503: Service unavailable (Miniserver restarting)
- 901: Maximum concurrent connections reached
- 409: Code already in use (user access codes)
- 406: Invalid code (user access codes)

## Exception Hierarchy
```kotlin
sealed class LoxoneException : Exception()
data class UnauthorizedException(override val message: String) : LoxoneException() // 401
data class ForbiddenException(override val message: String) : LoxoneException() // 403
data class NotFoundException(override val message: String) : LoxoneException() // 404
data class AuthTimeoutException(override val message: String) : LoxoneException() // 420
data class UserDisabledException(override val message: String) : LoxoneException() // 423
data class ServiceUnavailableException(override val message: String) : LoxoneException() // 503
data class MaxConnectionsException(override val message: String) : LoxoneException() // 901
```

## Testing
- Test exception creation for each code
- Test error message generation
- Test exception handling in clients

## Dependencies
None

## Acceptance Criteria
- [ ] All error codes defined
- [ ] Specific exceptions exist
- [ ] Error messages are clear
- [ ] All tests pass
```

---

## Phase 8: Additional Features

### Issue 17: Implement Icon/Image Download

**Labels:** `enhancement`, `protocol`, `priority:low`

```markdown
## Description
Implement downloading of icons and images (SVG and PNG) from the Miniserver using UUIDs from the structure file.

## Protocol Reference
- Document: `docs/loxone/CommunicatingWithMiniserver.md`
- Section: "Understanding the messages" → "Icons"
- Protocol Version: 16.0

## Current Status
No icon download support exists.

## Implementation Tasks
- [ ] Add icon download command by UUID
- [ ] Support SVG format (.svg extension)
- [ ] Support PNG format (.png or no extension)
- [ ] Implement icon caching
- [ ] Add cache invalidation
- [ ] Support custom icons
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update documentation

## Technical Details
- Icons identified by UUID (e.g., "00000000-0000-0020-2000000000000000.svg")
- SVG is newer format (Config 6.0+)
- PNG is legacy format
- Icons from structure file
- Binary file download

## API Design
```kotlin
interface IconRepository {
    suspend fun downloadIcon(uuid: String, format: IconFormat = IconFormat.SVG): ByteArray
    suspend fun getCachedIcon(uuid: String): ByteArray?
    suspend fun clearCache()
}

enum class IconFormat {
    SVG, PNG
}
```

## Testing
- Mock icon download
- Test SVG format
- Test PNG format
- Test caching
- Test with real Miniserver

## Dependencies
- Issue #3 (binary file download)
- Issue #4 (structure file for UUIDs)

## Acceptance Criteria
- [ ] Icons can be downloaded
- [ ] Both SVG and PNG supported
- [ ] Caching works
- [ ] All tests pass
```

---

### Issue 18: Implement Statistics Data Download

**Labels:** `enhancement`, `protocol`, `priority:low`

```markdown
## Description
Implement downloading of statistics data files from the Miniserver for historical data analysis.

## Protocol Reference
- Document: `docs/loxone/CommunicatingWithMiniserver.md`
- Section: "Understanding the messages" → "Binary Files"
- Protocol Version: 16.0

## Current Status
No statistics support exists.

## Implementation Tasks
- [ ] Research statistics data format
- [ ] Add statistics download commands
- [ ] Create statistics data model
- [ ] Implement binary parser for statistics
- [ ] Add time range selection
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update documentation

## Technical Details
- Statistics are binary files
- Format needs to be researched (not fully documented)
- May need reverse engineering
- Large files possible
- Time-series data

## API Design (tentative)
```kotlin
interface StatisticsRepository {
    suspend fun downloadStatistics(
        uuid: String,
        from: Instant,
        to: Instant
    ): StatisticsData
}

data class StatisticsData(
    val uuid: String,
    val entries: List<StatisticsEntry>
)

data class StatisticsEntry(
    val timestamp: Instant,
    val value: Double
)
```

## Testing
- Mock statistics file
- Test parsing
- Test time ranges
- Test with real Miniserver if possible

## Dependencies
- Issue #3 (binary file download)

## Acceptance Criteria
- [ ] Statistics can be downloaded
- [ ] Binary format parsed
- [ ] Data model works
- [ ] All tests pass
```

---

### Issue 19: Implement Visualization Password Support

**Labels:** `enhancement`, `protocol`, `priority:low`

```markdown
## Description
Implement support for visualization passwords, which allow limited access to specific controls via iOS and other visualization-specific commands.

## Protocol Reference
- Document: `docs/loxone/CommunicatingWithMiniserver.md`
- Section: "General Info" → "Secured Commands"
- Protocol Version: 16.0

## Current Status
No visualization password support exists.

## Implementation Tasks
- [ ] Add `jdev/sys/getvisusalt/{user}` command
- [ ] Add `jdev/sps/ios/{hash}/{uuid}/{command}` command support
- [ ] Add `jdev/sps/checkuservisupwd/{hash}` command
- [ ] Implement visualization password hashing
- [ ] Create VisualizationAuth class
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update documentation

## Technical Details
- Visualization passwords are separate from user passwords
- Used for limited access scenarios
- iOS command format: `jdev/sps/ios/{hash}/{uuid}/{command}`
- Hash is HMAC of command using visu salt

## Use Cases
- Mobile apps with limited access
- Public displays
- Guest access

## Testing
- Mock visu salt retrieval
- Test hashing
- Test iOS command
- Test password check

## Dependencies
None

## Acceptance Criteria
- [ ] Visualization salt retrieval works
- [ ] iOS command works
- [ ] Password check works
- [ ] All tests pass
```

---

### Issue 20: Optimize Message Header Handling

**Labels:** `enhancement`, `protocol`, `priority:low`, `performance`

```markdown
## Description
Implement adaptive timeouts and proper handling of estimated vs exact message headers to improve connection quality detection and performance.

## Protocol Reference
- Document: `docs/loxone/CommunicatingWithMiniserver.md`
- Section: "Understanding the messages" → "Message Header"
- Protocol Version: 16.0

## Current Status
- Message header parsing exists
- Estimated flag recognized but not used
- Fixed timeouts used

## Implementation Tasks
- [ ] Implement adaptive timeout based on message size
- [ ] Handle estimated header followed by exact header
- [ ] Use message size for connection quality detection
- [ ] Add keepalive timing adjustment based on message size
- [ ] Add connection quality metrics
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update documentation

## Technical Details
- 3rd byte, 1st bit: Estimated flag
- Estimated header always followed by exact header
- Use exact size for timeout calculation
- Large messages need longer timeouts
- Connection quality: keepalive RTT vs message size

## Adaptive Timeout Strategy
```kotlin
fun calculateTimeout(messageSize: Long, isEstimated: Boolean): Duration {
    val baseTimeout = 10.seconds
    val sizeMultiplier = (messageSize / 1024.0 / 1024.0) // MB
    val additionalTime = (sizeMultiplier * 5).seconds
    val buffer = if (isEstimated) 5.seconds else 0.seconds
    return baseTimeout + additionalTime + buffer
}
```

## Connection Quality Detection
- Measure keepalive RTT
- Consider ongoing large message transfers
- Adjust timeouts dynamically
- Warn user on poor connection

## Testing
- Mock large messages
- Test estimated vs exact headers
- Test timeout calculation
- Test connection quality metrics

## Dependencies
None

## Acceptance Criteria
- [ ] Adaptive timeouts work
- [ ] Estimated headers handled properly
- [ ] Connection quality detection works
- [ ] All tests pass
```

---

## Summary

This document provides 20 ready-to-use GitHub issue templates covering all missing features identified in the protocol gap analysis. Issues are organized into 8 phases:

1. **Phase 1 (Issues 1-3):** Core event processing - CRITICAL
2. **Phase 2 (Issues 4-5):** Structure file and control commands - CRITICAL  
3. **Phase 3 (Issues 6-8):** Command encryption - HIGH PRIORITY
4. **Phase 4 (Issue 9):** Certificate handling - MEDIUM PRIORITY
5. **Phase 5 (Issues 10-12):** Extended token management - MEDIUM PRIORITY
6. **Phase 6 (Issues 13-14):** CloudDNS discovery - MEDIUM PRIORITY
7. **Phase 7 (Issues 15-16):** Error handling - LOW PRIORITY
8. **Phase 8 (Issues 17-20):** Additional features - LOW PRIORITY

Each issue includes:
- Clear description
- Protocol references
- Implementation tasks checklist
- Technical details
- API design suggestions
- Testing requirements
- Dependencies
- Acceptance criteria

Copy the desired issue template, paste into GitHub Issues, and adjust as needed for your project's conventions.
