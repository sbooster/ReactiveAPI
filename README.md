<p align="center">
<h3 align="center">ReactiveAPI</h3>

------

<p align="center">
Gradle plugin to generate JSON describing Controller that containing MessageMapping annotation for RSocket.
</p>

<p align="center">
<img alt="License" src="https://img.shields.io/github/license/lovinadate/ReactiveAPI">
<a href="https://docs.gradle.org/7.5/release-notes.html"><img alt="" src="https://img.shields.io/badge/Gradle-7.5-brightgreen.svg?colorB=469C00&logo=gradle"></a>
<a href="https://discord.gg/P7FaqjcATp" target="_blank"><img alt="Discord" src="https://img.shields.io/discord/925686623222505482?label=discord"></a>
</p>

------

# Versioning

We use [Semantic Versioning 2.0.0](https://semver.org/spec/v2.0.0.html) to manage our releases.

# Features

- [X] Easy to use
- [X] Easy to configure
- [X] Lightweight

# Download

Add plugins repository in your settings.gradle:

```kotlin
dependencyResolutionManagement {
    pluginManagement {
        repositories {
            maven("https://repo.animecraft.fun/repository/maven-snapshots/")
        }
    }
}
```

Apply plugin in your build.gradle:

```kotlin
plugins {
    id("dev.socialbooster.gradle.reactiveapi") version "<version>"
}
```

# How To

* Apply plugin [as shown above](#Download)
* Run `gradle generateReactiveAPI`
* After task finished execution documented API can be found in outputFile (`$buildDir/libs/ReactiveAPI.json` by default)
* Output file can be specified by adding `outputFile = "<path to output file>"` in generateReactiveAPI task configuration
* Pretty Print option can be enabled by adding `prettyPrint = true` in generateReactiveAPI task configuration
