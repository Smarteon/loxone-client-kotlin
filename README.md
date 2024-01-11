# Loxone Kotlin Client
Experimental Kotlin implementation of the Loxone™ communication protocol.

* *Supported miniservers*: miniserver gen. 1, miniserver gen. 2, miniserver GO
* *Supported firmware*: **10.4.0.0** and ongoing

Most of the library is trying to behave according to 
[Loxone API documentation](https://www.loxone.com/enen/kb/api/) 
and [Loxone webservices](https://www.loxone.com/enen/kb/web-services/).  

*Disclaimer:*
This is an experimental project, which means:
* no further development is guaranteed
* there can be serious bugs 

Any feedback or help is welcomed.
 
## Usage
Currently, there is no artifact published to maven central, so in order to use it first build and publish it locally:
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
```
compile group: 'cz.smarteon.loxone', name: 'loxone-client-kotlin', version: 'desired version'
```
or
```kotlin
implementation("cz.smarteon.loxone", "loxone-client.kotlin", "desired version")
```
