# Loxone Protocol Coverage - Gap Analysis

**Document Version:** 1.1  
**Analysis Date:** 2026-06-19  
**Target Protocol Version:** 16.0  
**Repository:** loxone-client-kotlin

---

## Executive Summary

This document analyzes the current implementation of the loxone-client-kotlin library against the official Loxone communication protocol specification (docs/loxone/CommunicatingWithMiniserver.md). It identifies gaps in protocol coverage and proposes a prioritized list of features to implement.

The library currently implements core authentication (token-based), basic WebSocket communication, and keep-alive functionality. However, many protocol features remain unimplemented, including event processing, command encryption, binary message handling, and various commands.

---

## Current Implementation Status

### ✅ Implemented Features

1. **Connection Management**
   - WebSocket connection (RFC6455)
   - HTTP client support
   - Keep-alive mechanism
   - Basic endpoint configuration (HTTP/HTTPS, WS/WSS)

2. **Authentication & Authorization**
   - Token-based authentication (JWT)
   - Token acquisition (`getjwt`)
   - Token authentication (`authwithtoken`)
   - Token killing (`killtoken`)
   - Token refresh tracking (expiry detection)
   - Hashing support (SHA1, SHA256, HMAC)
   - getkey2 command for user salt retrieval
   - Token permission model (TokenPermission)
   - RSA public key retrieval (`jdev/sys/getPublicKey`) — PR #75
   - RSA PKCS1v1.5 encryption, multiplatform (JVM/Native/JS) — PR #75
   - Public key caching (`PublicKeyRepository`) — PR #75

3. **Basic Commands**
   - API info retrieval (`jdev/cfg/api`)
   - API key info retrieval (`jdev/cfg/apiKey`)
   - Keep-alive command
   - Raw command execution

4. **Message Handling**
   - LoxoneMsg parsing (JSON responses)
   - Message header parsing (binary protocol)
   - Text message reception
   - Error code handling (401, 400, 404, 420, 500)

5. **Infrastructure**
   - Multiplatform support (JVM, JS, Linux Native)
   - Ktor client integration
   - kotlinx.serialization for JSON
   - Proper coroutine-based async operations

---

## Missing Features & Gaps

### 🔴 Critical Gaps (Core Protocol Features)

#### 1. Binary Event Processing
**Status:** Not Implemented  
**Impact:** High - Cannot process real-time state updates from Miniserver  
**Protocol Reference:** CommunicatingWithMiniserver.md, "Understanding the messages" → "Event-Tables"

**Missing Components:**
- Event-Table of Value-States (MessageKind.EVENT_VALUE)
  - Binary structure: UUID (128-bit) + double value (64-bit)
  - 24 bytes per event
- Event-Table of Text-States (MessageKind.EVENT_TEXT)
  - Binary structure: UUID + UUID-Icon + text length + text + padding
- Event-Table of Daytimer-States (MessageKind.EVENT_DAYTIMER)
  - Binary structure: UUID + default value + nEntries + entries array
- Event-Table of Weather-States (MessageKind.EVENT_WEATHER)
  - Binary structure: last update timestamp + nEntries + entries array
- Binary file downloads (MessageKind.FILE)
- Out-of-service indicator handling (MessageKind.OUT_OF_SERVICE)

**Current Code Reference:** 
- `src/commonMain/kotlin/message/MessageHeader.kt` - MessageKind enum exists but no parsers
- `src/commonMain/kotlin/ktor/KtorWebsocketLoxoneClient.kt:129` - TODO comment for binary message processing

#### 2. Enable Binary Status Updates
**Status:** Not Implemented  
**Impact:** High - Required to receive event tables  
**Protocol Reference:** CommunicatingWithMiniserver.md, "Handling an active connection"

**Missing Components:**
- Command: `jdev/sps/enablebinstatusupdate`
- This command must be sent after authentication to start receiving events
- Without this, the miniserver won't send event updates via WebSocket

#### 3. Structure File Support
**Status:** Partially Implemented  
**Impact:** High - Required for control discovery and state mapping  
**Protocol Reference:** CommunicatingWithMiniserver.md, "Structure-File: LoxAPP3.json"

**Missing Components:**
- Download command: `data/LoxAPP3.json`
- Version check command: `jdev/sps/LoxAPPversion3`
- Structure file parsing (controls, rooms, categories, UUIDs)
- Caching mechanism based on lastModified timestamp
- Note: Separate documentation exists for structure file format

#### 4. Command Encryption
**Status:** Implemented (WebSocket)  
**Impact:** High - Required for secure command transmission  
**Protocol Reference:** CommunicatingWithMiniserver.md, "Command Encryption"

**Implemented (PR #75):**
- Public key retrieval: `jdev/sys/getPublicKey` ✅
- RSA encryption (PKCS1v1.5, Base64 NoWrap), multiplatform ✅

**Implemented (#76):**
- AES-256-CBC key + IV generation (random, 32-byte key / 16-byte IV), multiplatform ✅
- AES-256-CBC encryption/decryption with ZeroBytePadding ✅
- Key exchange: `jdev/sys/keyexchange/{encrypted-session-key}` ✅
- Encrypted command wrapping: `jdev/sys/enc/{cipher}` ✅
- Encrypted command with encrypted response: `jdev/sys/fenc/{cipher}` ✅
- Session salt rotation (anti-replay) per command ✅
- Mandatory encryption of token acquisition (`getjwt`/`authwithtoken`); opt-in
  `CommandEncryption` mode (NONE/REQUEST/REQUEST_RESPONSE) for all other commands ✅
- Public key fetched over **HTTP** (`jdev/sys/getPublicKey` is rejected on the websocket with 400);
  the Miniserver returns it wrapped in `-----BEGIN CERTIFICATE-----` markers, normalized to a
  `PUBLIC KEY` PEM by `LoxoneCrypto.normalizePublicKeyPem` ✅

**Remaining / known limitations:**
- HTTP command encryption (`?sk=` session-key query form) — only the WebSocket channel is wired
- Public key caching via `PublicKeyRepository` (currently fetched once per session); note the
  dormant `PublicKey` message model from PR #75 assumes a `{"publicKey":...}` object value and is
  unused by the websocket flow
- `REQUEST_RESPONSE` (fenc) mode is unsafe with binary/large responses (images, files, `LoxAPP3.json`)
  — the Miniserver only supports `enc` for those
- No reconnect: after `close()` (incl. keepalive timeout) the client is single-use
- The JS `actual` (crypto-js) is not exercised by tests (the JS test task is disabled); the common
  AES roundtrip would cover it once a JS test target is enabled
- **Fixed AES-CBC IV**: the IV is generated once and reused for the entire session. Salt rotation
  provides per-command plaintext uniqueness, but reusing the same key+IV in AES-CBC is a protocol
  limitation — the Miniserver's key-exchange API does not support IV rotation.

**Current Code Reference:**
- `src/commonMain/kotlin/LoxoneCrypto.kt` — hashing, RSA + AES (`aesEncrypt`/`aesDecrypt`)
- `src/commonMain/kotlin/ktor/KtorWebsocketLoxoneClient.kt` — key exchange + enc/fenc wrapping
- `src/commonMain/kotlin/LoxoneClientSettings.kt` — `CommandEncryption` setting
- `src/commonMain/kotlin/message/PublicKey.kt` — public key model
- `src/commonMain/kotlin/PublicKeyRepository.kt` — key caching

---

### 🟡 Important Gaps (Extended Functionality)

#### 5. Control Commands
**Status:** Not Implemented  
**Impact:** Medium-High - Cannot control devices  
**Protocol Reference:** CommunicatingWithMiniserver.md, "General Info" → "Commands"

**Missing Components:**
- Generic control command: `jdev/sps/io/{uuid}/{command}`
- Control command support for various control types
- Command response handling (state after execution)

#### 6. Certificate Handling
**Status:** Not Implemented  
**Impact:** Medium - Required for TLS verification  
**Protocol Reference:** CommunicatingWithMiniserver.md, "Setting up a connection"

**Missing Components:**
- Certificate retrieval: `jdev/sys/getcertificate`
- Certificate chain verification
- Loxone Root Certificate validation
- Public key extraction from certificate

#### 7. Token Management Extensions
**Status:** Partially Implemented  
**Impact:** Medium - Limited token lifecycle management  
**Protocol Reference:** CommunicatingWithMiniserver.md, "Tokens"

**Missing Components:**
- Token refresh command: `jdev/sys/refreshjwt/{tokenHash}/{user}`
- Token validity check: `jdev/sys/checktoken/{tokenHash}/{user}`
- Support for plaintext token in refresh (since 11.2)
- Token state persistence across sessions

**Current Code Reference:**
- `src/commonMain/kotlin/message/TokenState.kt` - Refresh detection exists but no command
- `src/commonMain/kotlin/LoxoneCommands.kt` - Only get, auth, kill implemented

#### 8. CloudDNS Support
**Status:** Not Implemented  
**Impact:** Medium - Cannot discover Miniserver via CloudDNS  
**Protocol Reference:** CommunicatingWithMiniserver.md, "General Info" → "CloudDNS"

**Missing Components:**
- CloudDNS lookup: `dns.loxonecloud.com/?getip&snr={SNR}&json=true`
- Response parsing (IP, port, code, PortOpen, LastUpdated, IPHTTPS, etc.)
- Error code handling (403, 405, 409, 412, 418, 481, 482, 483)
- HTTPS/WSS support detection
- Remote Connect Service support

#### 9. Icon/Image Download
**Status:** Not Implemented  
**Impact:** Low-Medium - Cannot display visual elements  
**Protocol Reference:** CommunicatingWithMiniserver.md, "Understanding the messages" → "Icons"

**Missing Components:**
- SVG icon download by UUID
- PNG icon download (legacy)
- Binary file download handling
- Image caching

#### 10. Statistics Data
**Status:** Not Implemented  
**Impact:** Low - Cannot retrieve historical data  
**Protocol Reference:** CommunicatingWithMiniserver.md, "Understanding the messages" → "Binary Files"

**Missing Components:**
- Statistics data download commands
- Binary statistics file parsing

---

### 🟢 Nice-to-Have Features

#### 11. WebSocket Close Code Handling
**Status:** Partially Implemented  
**Impact:** Low-Medium - Better error diagnostics  
**Protocol Reference:** CommunicatingWithMiniserver.md, "Websocket Close Codes"

**Missing Components:**
- Custom close code interpretation:
  - 4003: Too many failed login attempts / blocked
  - 4004: Some user has been changed
  - 4005: Current user has been changed
  - 4006: User has been disabled
  - 4007: Miniserver performing update
  - 4008: No event slots available

**Current Code Reference:**
- `src/commonMain/kotlin/ktor/KtorWebsocketLoxoneClient.kt` - Basic close handling exists

#### 12. Error Code Extensions
**Status:** Partially Implemented  
**Impact:** Low - Better error handling  
**Protocol Reference:** CommunicatingWithMiniserver.md, "Error-Codes"

**Current Code Reference:**
- `src/commonMain/kotlin/message/LoxoneMsg.kt` - Some codes defined (200, 400, 401, 404, 420, 500)

**Missing Error Codes:**
- 403: Insufficient rights
- 423: User disabled
- 503: Service unavailable (restarting)
- 901: Max concurrent connections
- 409: Code already in use (user access codes)
- 406: Invalid code

#### 13. Visualization Password Support
**Status:** Not Implemented  
**Impact:** Low - Limited use case  
**Protocol Reference:** CommunicatingWithMiniserver.md, "General Info" → "Secured Commands"

**Missing Components:**
- Visualization salt: `jdev/sys/getvisusalt/{user}`
- iOS command: `jdev/sps/ios/{hash}/{uuid}/{command}`
- Visualization password check: `jdev/sps/checkuservisupwd/{hash}`

#### 14. HTTP/HTTPS Status Detection
**Status:** Partially Implemented  
**Impact:** Low - Already using endpoint configuration  
**Protocol Reference:** CommunicatingWithMiniserver.md, "Using HTTPS/WSS"

**Current Code Reference:**
- `src/commonMain/kotlin/message/ApiKeyInfo.kt` - httpsStatus field exists

**Enhancement Needed:**
- Automatic protocol selection based on httpsStatus
- TLS certificate hostname verification
- Local connection detection and handling

#### 15. Remote Connect Service
**Status:** Not Implemented  
**Impact:** Low - Specific use case  
**Protocol Reference:** CommunicatingWithMiniserver.md, "General Info" → "Remote Connect Service"

**Missing Components:**
- Support for distributed data centers
- Remote connect protocol handling
- Automatic fallback to CloudDNS

#### 16. Message Size Estimation
**Status:** Partially Implemented  
**Impact:** Low - Performance optimization  
**Protocol Reference:** CommunicatingWithMiniserver.md, "Understanding the messages" → "Message Header"

**Current Code Reference:**
- `src/commonMain/kotlin/message/MessageHeader.kt` - sizeEstimated field exists

**Enhancement Needed:**
- Adaptive timeout based on estimated vs exact size
- Proper handling of estimated headers followed by exact headers

---

## Proposed Implementation Roadmap

### Phase 1: Core Event Processing (High Priority)
**Goal:** Enable real-time state monitoring from Miniserver

1. **Issue: Implement Binary Event-Table Parsing**
   - Parse Value-State events (UUID + double)
   - Parse Text-State events (UUID + UUID-Icon + text)
   - Parse Daytimer-State events
   - Parse Weather-State events
   - Create event listener/callback mechanism

2. **Issue: Implement Enable Binary Status Updates Command**
   - Add `jdev/sps/enablebinstatusupdate` command
   - Integrate into WebSocket client initialization flow
   - Handle binary message reception in WebSocket client

3. **Issue: Complete Binary File Download Support**
   - Implement `callRawForData` in WebSocket client
   - Handle FILE message kind properly
   - Add binary response buffering

### Phase 2: Structure File & Control Commands (High Priority)
**Goal:** Enable control discovery and device manipulation

4. **Issue: Implement Structure File Download and Caching**
   - Add `data/LoxAPP3.json` download command
   - Add `jdev/sps/LoxAPPversion3` version check command
   - Implement caching with lastModified comparison
   - Parse basic structure file format (defer full parsing to separate issue)

5. **Issue: Implement Control Commands**
   - Add generic `jdev/sps/io/{uuid}/{command}` support
   - Create typed command builders for common control types
   - Handle command response parsing

### Phase 3: Command Encryption (High Priority - Security)
**Goal:** Enable secure command transmission

6. ~~**Issue: Implement RSA Public Key Retrieval and Management**~~ ✅ **Done — PR #75, closes #28**
   - ~~Add `jdev/sys/getPublicKey` command~~
   - ~~Add RSA encryption support (requires crypto library evaluation)~~
   - ~~Implement key storage and reuse~~

7. **Issue: Implement AES Command Encryption and WebSocket Key Exchange**
   - Add AES-256-CBC key + IV generation
   - Add AES-256-CBC encryption/decryption with ZeroBytePadding (multiplatform)
   - Add `jdev/sys/keyexchange/{encrypted-session-key}` — wire RSA-encrypted session key exchange into WebSocket connection setup
   - Implement `jdev/sys/enc/{cipher}` command wrapper with salt rotation
   - Implement `jdev/sys/fenc/{cipher}` with AES-decrypted response

### Phase 4: Certificate & TLS Support (Medium Priority)
**Goal:** Proper TLS verification and security

9. **Issue: Implement Certificate Retrieval and Verification**
   - Add `jdev/sys/getcertificate` command
   - Implement certificate chain verification
   - Add Loxone Root Certificate validation
   - Extract and store public key from certificate

### Phase 5: Token Management Extensions (Medium Priority)
**Goal:** Complete token lifecycle management

10. **Issue: Implement Token Refresh Command**
    - Add `jdev/sys/refreshjwt/{tokenHash}/{user}` command
    - Support both hashed and plaintext token (11.2+)
    - Integrate automatic refresh in TokenAuthenticator

11. **Issue: Implement Token Validity Check**
    - Add `jdev/sys/checktoken/{tokenHash}/{user}` command
    - Create token validation utility

12. **Issue: Implement Token Persistence**
    - Add TokenRepository implementation for storage
    - Support token save/load across sessions
    - Handle token revocation and cleanup

### Phase 6: CloudDNS & Discovery (Medium Priority)
**Goal:** Automatic Miniserver discovery

13. **Issue: Implement CloudDNS Client**
    - Create CloudDNS lookup client
    - Parse CloudDNS response (IP, port, status, etc.)
    - Handle all CloudDNS error codes
    - Support HTTPS/WSS detection

14. **Issue: Implement Automatic Endpoint Resolution**
    - Create factory method for CloudDNS-based endpoint creation
    - Implement automatic HTTP/HTTPS protocol selection
    - Add Remote Connect Service support

### Phase 7: Enhanced Error Handling (Low Priority)
**Goal:** Better diagnostics and user feedback

15. **Issue: Implement WebSocket Close Code Handling**
    - Add close code interpretation (4003-4008)
    - Create user-friendly error messages
    - Add retry logic for specific codes

16. **Issue: Extend Error Code Coverage**
    - Add missing error codes (403, 423, 503, 901, 409, 406)
    - Create error code to exception mapping
    - Add error recovery strategies

### Phase 8: Additional Features (Low Priority)
**Goal:** Complete protocol coverage

17. **Issue: Implement Icon/Image Download**
    - Add icon download by UUID (SVG/PNG)
    - Implement image caching mechanism
    - Support custom icons

18. **Issue: Implement Statistics Data Download**
    - Add statistics data download commands
    - Parse binary statistics format
    - Create statistics data model

19. **Issue: Implement Visualization Password Support**
    - Add `jdev/sys/getvisusalt/{user}` command
    - Add `jdev/sps/ios/{hash}/{uuid}/{command}` support
    - Add `jdev/sps/checkuservisupwd/{hash}` command

20. **Issue: Optimize Message Header Handling**
    - Implement adaptive timeouts based on message size
    - Handle estimated vs exact header sequences
    - Improve connection quality detection

---

## Implementation Dependencies

```
Phase 1 (Events) → No dependencies
Phase 2 (Structure/Controls) → Phase 1 (for state mapping)
Phase 3 (Encryption) → Requires crypto library selection
Phase 4 (Certificates) → Phase 3 (public key extraction)
Phase 5 (Token Extensions) → No dependencies (can be done in parallel)
Phase 6 (CloudDNS) → Phase 4 (HTTPS detection)
Phase 7-8 (Enhancements) → No critical dependencies
```

---

## Technical Considerations

### Multiplatform Compatibility
- **Binary Parsing:** Use kotlinx.io or similar for multiplatform ByteArray handling
- **Encryption:** Evaluate kotlincrypto (already used) vs platform-specific crypto
  - RSA support may require platform-specific implementations
  - AES-256-CBC support needs verification across platforms
- **Certificate Handling:** Platform-specific X.509 parsing may be needed

### Breaking Changes
Most implementations can be added without breaking existing APIs:
- New commands can be added to LoxoneCommands object
- Event processing can use callback/flow-based APIs
- Encryption can be opt-in via settings

### Testing Strategy
- **Unit Tests:** All new commands and message parsers
- **Integration Tests:** WebSocket flow with test server (ktor-server-test-host)
- **Acceptance Tests:** Against real Miniserver (jvmAcceptanceTest)
- Binary format tests with known byte sequences

### Documentation Updates
Each phase should include:
- API documentation (KDoc)
- Usage examples
- Update to main README.md
- Protocol mapping documentation

---

## Notes for Issue Creation

### Issue Template Structure
Each issue should follow this format:

```markdown
## Description
[Brief description of the feature from protocol perspective]

## Protocol Reference
- Document: CommunicatingWithMiniserver.md
- Section: [Specific section]
- Version: 16.0

## Implementation Tasks
- [ ] Add command definition to LoxoneCommands
- [ ] Implement request/response models
- [ ] Add client method
- [ ] Write unit tests
- [ ] Write integration tests
- [ ] Update documentation
- [ ] Add usage example

## Technical Details
[Binary format, encryption details, etc.]

## Testing
[How to verify the implementation]

## Dependencies
[Links to related issues]
```

### Labels to Use
- `enhancement` - New feature implementation
- `protocol` - Protocol specification related
- `priority:high` / `priority:medium` / `priority:low`
- `multiplatform` - Requires platform-specific handling
- `breaking-change` - API breaking changes
- `good-first-issue` - For simpler tasks (error codes, etc.)

---

## Conclusion

The loxone-client-kotlin library has a solid foundation with authentication, basic communication, and keep-alive functionality. However, to be a complete protocol implementation, it requires:

1. **Critical additions:** Event processing, structure file support, command encryption
2. **Important features:** Control commands, certificate handling, token management
3. **Enhancements:** CloudDNS, error handling, additional protocol features

The proposed 8-phase roadmap provides a clear path to full protocol coverage, with phases 1-3 being critical for a functional real-time control client.

**Estimated Effort:**
- Phase 1-2: ~4-6 weeks (core functionality)
- Phase 3-4: ~3-4 weeks (security features)
- Phase 5-6: ~2-3 weeks (extended features)
- Phase 7-8: ~2-3 weeks (polish and completeness)

**Total: ~11-16 weeks for complete implementation**

This analysis provides a comprehensive view of what's missing and a prioritized approach to achieving full Loxone protocol coverage in the Kotlin multiplatform library.
