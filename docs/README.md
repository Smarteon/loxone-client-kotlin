# Documentation

This directory contains documentation for the Loxone Kotlin Client library.

## Contents

- **[loxone/](loxone/)** - Loxone Miniserver communication protocol documentation
  - [CommunicatingWithMiniserver.md](loxone/CommunicatingWithMiniserver.md) - Official protocol specification (v16.0)
  
- **[protocol-gap-analysis.md](protocol-gap-analysis.md)** - Comprehensive analysis of protocol implementation gaps
  - Current implementation status
  - Missing features categorized by priority
  - 8-phase implementation roadmap
  - Technical considerations and dependencies
  - Estimated effort: 11-16 weeks for complete coverage

- **[github-issues-templates.md](github-issues-templates.md)** - Ready-to-use GitHub issue templates
  - 20 issues covering all missing protocol features
  - Organized into 8 implementation phases
  - Includes technical details, API designs, and acceptance criteria
  - Can be copied directly into GitHub Issues

## Quick Start for Contributors

1. **Understanding the Protocol:** Start with [loxone/CommunicatingWithMiniserver.md](loxone/CommunicatingWithMiniserver.md)
2. **Finding Gaps:** Review [protocol-gap-analysis.md](protocol-gap-analysis.md) for what's missing
3. **Picking Work:** Use [github-issues-templates.md](github-issues-templates.md) to create issues

## Implementation Priorities

### 🔴 Critical (Phase 1-2)
- Binary event processing (value, text, daytimer, weather)
- Enable binary status updates command
- Structure file download and caching
- Control commands

### 🟡 High Priority (Phase 3-4)
- Command encryption (AES + RSA)
- Certificate handling and verification
- WebSocket key exchange

### 🟢 Medium Priority (Phase 5-6)
- Token refresh and validation
- Token persistence
- CloudDNS discovery

### ⚪ Low Priority (Phase 7-8)
- Enhanced error handling
- Icon/image download
- Statistics data
- Visualization passwords

## Contributing

When adding new documentation:
1. Use Markdown format
2. Keep documentation up-to-date with code changes
3. Use clear section headers and examples where appropriate
4. Link to relevant source code when helpful
5. Update this README when adding new documentation files
