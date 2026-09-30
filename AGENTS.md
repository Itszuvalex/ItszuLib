# AGENTS.md

Guidance for coding agents (Claude Code, Codex, etc.) and humans working in this repository.

## What this is

ItszuLib is Itszuvalex's shared library mod: block entity and block base classes, inventories and storage, multiblocks, tile networks, containers/GUIs, and assorted utilities. Femtocraft (`../Femtocraft`) is built on it.

- **Minecraft 26.1.2 / NeoForge 26.1.2.112 / Java 25**, ModDevGradle (`net.neoforged.moddev` 2.0.148), Gradle 9.2.1.
- Written in **Kotlin 2.4.0**, loaded through **Kotlin for Forge 6.3.0** (`thedarkcolour:kotlinforforge-neoforge`, `modLoader="kotlinforforge"`). KFF is a required runtime mod: it provides the language loader and the Kotlin stdlib, reflect, coroutines and serialization. Do not add a second copy of the stdlib (`kotlin.stdlib.default.dependency=false`).
- Mod id `itszulib`, package `com.itszuvalex.itszulib`, GPL-2.0-or-later.
- Being ported from Forge 1.7.10 / Scala 2.11 on branch `neoforge-26.1` (from `develop`). `master`/`develop` still hold the 1.7.10 code. Port status: [docs/PORTING.md](docs/PORTING.md). Decisions and open questions: [docs/DECISIONS.md](docs/DECISIONS.md).

## Build and run

```bash
./gradlew build                 # compile + unit tests + jar (build/libs/itszulib-<version>.jar)
./gradlew test                  # JUnit unit tests only
./gradlew runGameTestServer     # in-game tests; exits non-zero if a required test fails
./gradlew runClient             # dev client (dev content is registered)
./gradlew runServer             # dev dedicated server (run/eula.txt must say eula=true)
```

`runGameTestServer` also runs one built-in vanilla test (`minecraft:default` environment), so its "All N required tests passed" count is ItszuLib's tests plus one. `runServer` does not forward stdin, so use game tests for in-world checks.

The build targets a JDK 25 toolchain, and NeoForge's tooling also uses JDK 21. Gradle auto-detects installed JDKs and downloads missing ones through the foojay resolver (`settings.gradle`). `gradlew` itself needs Java 17+ on `PATH` or `JAVA_HOME` to start.

Mod metadata is generated from `src/main/templates/META-INF/neoforge.mods.toml` using the `mod_*` and `kff_version` properties in `gradle.properties`.

## Machine notes

Machine-specific JDK settings go in the user-level `~/.gradle/gradle.properties` (or `$GRADLE_USER_HOME/gradle.properties`), never in the project's `gradle.properties`. NixOS can't run the generic-Linux JDKs Gradle downloads, so point it at Nix JDKs there:

```properties
org.gradle.java.home=/home/cchharris/.gradle/jdks/jdk25
org.gradle.java.installations.paths=/home/cchharris/.gradle/jdks/jdk25,/home/cchharris/.gradle/jdks/jdk21
org.gradle.java.installations.auto-download=false
```

On the maintainer's Windows machine `GRADLE_USER_HOME` is under scoop (`~/scoop/apps/gradle/current/.gradle`), not `~/.gradle`.

## Documentation

NeoForge's API changes a lot between versions and many online examples are stale. Prefer these sources, in order:

1. **Decompiled, NeoForge-patched Minecraft sources**: `build/moddev/artifacts/minecraft-patched-<version>-sources.jar` (created by any Gradle build). NeoForge classes: `neoforge-<version>-universal.jar` under `$GRADLE_USER_HOME/caches/modules-2/files-2.1/net.neoforged/neoforge/`; use `javap` to check signatures.
2. **NeoForge docs**: https://docs.neoforged.net/docs/ (unversioned pages are 26.1): capabilities, transactions, Value I/O, block entities, menus, networking, game tests.
3. **Porting primers**: https://docs.neoforged.net/primer/docs/ (per-version change lists).
4. **ModDevGradle docs**: https://docs.neoforged.net/toolchain/docs/plugins/mdg/.
5. **Kotlin for Forge**: https://github.com/thedarkcolour/KotlinForForge (branch `6.x` for 1.21.9–26.2).
6. **Forge 1.7.10 era** (historical, for the legacy code's intent only).

Reference implementation for 26.1 patterns (same author, Java): `../technolich` on branch `neoforge-26.1` (Value I/O serialization, transfer-API adapters, networks, game tests).

## Source layout

```
src/main/kotlin/com/itszuvalex/itszulib/
├── ItszuLib.kt        @Mod object (KFF); registers dev content outside production
└── dev/               Dev-only content and game tests (never registered in production)
src/test/kotlin/...    JUnit 5 tests
src/main/scala/...     LEGACY 1.7.10 Scala sources, not compiled; reference only, deleted as areas are ported
src/test/scala/...     LEGACY ScalaTest suites, reference only
```

## Kotlin conventions

- `@Mod` classes are Kotlin `object`s; use `thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS` for the mod event bus and `NeoForge.EVENT_BUS` for game events.
- Registries: `DeferredRegister` in `object`s, registered from the mod object's `init`.
- Use `@JvmField`/`@JvmStatic` on things Java code or reflection needs to see as plain fields/statics.
- Nullability: Kotlin types carry it; prefer non-null returns and `?` only where NeoForge expects a nullable (capability providers).

## Dev content and game tests

`dev/DevGameTests.kt` registers test functions (`Registries.TEST_FUNCTION`) and test instances (`RegisterGameTestsEvent`) on vanilla's 1x1x1 `minecraft:empty` structure. Add a test with `test("name") { helper -> ...; helper.succeed() }`. `GameTestHelper#assertValueEqual(expected, actual, name)` takes the expected value first.

## Testing conventions

- Unit tests: `src/test/kotlin`, JUnit 5, names like `method_ExpectedBehavior`. ModDevGradle puts Minecraft classes on the test classpath, but anything needing registries or a level belongs in a game test.
- Add a game test for anything that crosses into vanilla/NeoForge (registries, block entities, capabilities, serialization with real items, networking).
- Verify with `./gradlew build` **and** `./gradlew runGameTestServer`.
