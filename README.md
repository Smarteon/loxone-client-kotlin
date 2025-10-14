# Loxone Kotlin Client [![Maven Central](https://maven-badges.herokuapp.com/maven-central/cz.smarteon.loxone/loxone-client-kotlin/badge.svg)](https://maven-badges.herokuapp.com/maven-central/cz-smarteon-loxone/loxone-client-kotlin) [![codecov](https://codecov.io/gh/Smarteon/loxone-client-kotlin/branch/master/graph/badge.svg)](https://codecov.io/gh/Smarteon/loxone-client-kotlin)
Experimental Kotlin implementation of the Loxone™ communication protocol.

* *Supported miniservers*: miniserver gen. 1, miniserver gen. 2, miniserver GO
* *Supported firmware*: **10.4.0.0** and ongoing

Most of the library is trying to behave according to 
[Loxone API documentation](https://www.loxone.com/enen/kb/api/) 
and [Loxone webservices](https://www.loxone.com/enen/kb/web-services/).

For the detailed Loxone communication protocol specification, see the [documentation](docs/loxone/) which includes a markdown version of the official PDF.

## Protocol Coverage

This library implements core features of the Loxone protocol including token-based authentication, WebSocket/HTTP communication, and keep-alive functionality. For a comprehensive analysis of protocol coverage and planned features, see:

- **[Protocol Gap Analysis](docs/protocol-gap-analysis.md)** - Detailed comparison with the official protocol
- **[GitHub Issue Templates](docs/github-issues-templates.md)** - Ready-to-use templates for implementing missing features

Key implemented features:
- ✅ Token-based authentication (JWT)
- ✅ WebSocket and HTTP clients
- ✅ Keep-alive mechanism
- ✅ Basic API commands
- ✅ Multiplatform support (JVM, JS, Linux Native)

Notable gaps (contributions welcome):
- ⏳ Binary event processing (real-time state updates)
- ⏳ Structure file parsing
- ⏳ Command encryption (AES + RSA)
- ⏳ Control commands

See the [gap analysis](docs/protocol-gap-analysis.md) for complete details and implementation roadmap.

*Disclaimer:*
This is an experimental project, which means:
* no further development is guaranteed
* there can be serious bugs 

Any feedback or help is welcomed.

The predecessor of this project is [loxone-java](https://github.com/Smarteon/loxone-java) library. For more complete experience on JVM, consider using it. There is a plan to merge both projects in the future in some way.
 
## Usage
In order to use version from master which is not released, it's needed build and publish it locally first:
```bash
./gradlew build publishToMavenLocal
```

### Maven
```xml
<dependency>
    <groupId>cz.smarteon.loxone</groupId>
    <artifactId>loxone-client-kotlin</artifactId>
    <version><!-- desired version --></version>
</dependency>
```

### Gradle
```groovy
compile group: 'cz.smarteon.loxone', name: 'loxone-client-kotlin', version: 'desired version'
```
or
```kotlin
implementation("cz.smarteon.loxone", "loxone-client.kotlin", "desired version")
```
