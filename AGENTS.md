# AGENTS.md

Guidance for coding agents (Claude Code, Codex, etc.) and humans working in this repository.

## What this is

ItszuLib is Itszuvalex's shared library mod. On this branch it is the **Kotlin version of TechnoLich's block entity framework**: block entities composed of fragments, a module/capability layer, scoped serialization, item/energy storage abstractions with NeoForge transfer-API adapters, and block entity networks. Femtocraft (`../Femtocraft`) is built on it. TechnoLich (`../technolich`) is being moved onto the same Kotlin code; keep the two in sync (see [docs/DECISIONS.md](docs/DECISIONS.md) B1).

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

Framework source of truth: `../technolich` on branch `neoforge-26.1` (its `AGENTS.md` describes the same concepts). ItszuLib was synced to technolich@f021246.

## Source layout

```
src/main/kotlin/com/itszuvalex/itszulib/
├── ItszuLib.kt            @Mod object (KFF): module init, data components, dev content (non-production only),
│                          server tick / chunk unload / server stop -> NETWORK_MANAGER
├── api/
│   ├── Api.kt             Capabilities (COLORABLE), Modules (COLORABLE; Modules.init()), Components
│   │                      (FRAGMENT_DATA = itszulib:fragment_data), ModuleCapabilities (registers a BlockEntityCore
│   │                      type's modules + STANDARD NeoForge caps)
│   ├── adapters/          Engine-facing interfaces: IModule/Module, IModuleProvider, IBlockEntity, ILevel,
│   │                      IItemStack, IBattery, IColorable
│   ├── storage/           IItemStorage + implementations (Array, Slice, Aggregate, NBT, Dynamic,
│   │                      ResourceHandler-backed); IBattery implementations (PowerBattery, PowerBatteryNBT,
│   │                      DynamicIBattery, EnergyHandler-backed)
│   ├── utility/           Loc4 (+Level/ILevel/Indirect), ChunkCoord, LocationTracker, DirectionUtil, module
│   │                      capability maps, IScopedSerialization + NBTSerializationScope, Overideable, sided holders
│   └── wrappers/          Vanilla/NeoForge <-> ItszuLib adapters (WrapperLevel, WrapperBlockEntity,
│                          WrapperVanillaItemStack, WrapperContainerIItemStorage,
│                          WrapperResourceHandlerIItemStorage, WrapperEnergyHandlerIBattery, WrapperCache)
├── core/                  BlockEntityCore, TickableBlockEntityCore, EntityBlockCore, TickableEntityBlockCore,
│   │                      BlockEntityFragmentCollection + fragment interfaces (Fragments.kt), networks
│   │                      (Networks.kt), SidedStorageConfiguration
│   └── frag/              Fragment base classes, FragColorable, FragDropInventory
├── dev/                   Dev-only block + game tests (never registered in production)
└── util/                  Color, InventoryUtils (item dropping), Singleton
src/test/kotlin/...        JUnit tests + Testable* fakes that avoid vanilla objects (TestHelpers.kt, CoreTests.kt)
```

The 1.7.10 Scala sources were removed on this branch (the framework replaced them); they remain on `develop`.

## Core concepts

The concepts are TechnoLich's; see `../technolich/AGENTS.md` for the long form. Kotlin differences: lookups return nullable `T?` instead of `Optional<T>`, collections of locations/nodes are `Sequence`s, and `Loc4.x/y/z/pos/dimensionId`, `IModule.id/blockCapability/itemCapability` and `Color.alpha/red/green/blue` are properties.

### Fragments and BlockEntityCore
A `BlockEntityCore` owns a `BlockEntityFragmentCollection` (`fragList`). Compose behaviour in the block entity's `init`:

- `fragList.addFragment(IBlockEntityFragment<T>)`: a fragment that exposes a module `T` (e.g. `FragColorable` exposes itself as `IColorable`). `faceToModuleMapper(be)` maps a nullable `Direction` to the live instance (or null). Expose an interface whose mutators save/sync, never a mutable value object.
- `fragList.addInternalFragment(IInternalBlockEntityFragment)`: hooks without an exposed module (serialization, `onRemove`, e.g. `FragDropInventory`).
- Fragment `name()`s must be unique per block entity (they key saved data) and each module may be exposed by one fragment only; both throw `IllegalArgumentException`.
- Fragments get an `IFragmentHost` in `onAttach`. `InternalBlockEntityFragment`/`BlockEntityFragment` subclasses call `markDirty()` after changing saved state and `markDirtyAndSync()` after changing client-visible (DESCRIPTION) state.
- Storages report their own changes: pass an `onChanged` runnable (e.g. `{ markDirty() }`) to `ItemStorageArray`/`ItemStorageNBT`/`PowerBattery`. `setSlotQuietly`/`setStorageQuietly` skip it; the NeoForge adapters use them inside transactions and call `setChanged()` once on root commit.
- `fragList.addCapability(cap) { side -> ... }` exposes a non-module capability, typically one of `ModuleCapabilities.STANDARD` (NeoForge item/fluid/energy).
- `fragList.addTickable(...)`: ticked by `TickableBlockEntityCore` when the block's `TickableEntityBlockCore#hasTicker(side)` returns true.

`BlockEntityCore` wires fragments into the vanilla lifecycle: `saveAdditional`/`loadAdditional` (LEVEL), `getUpdateTag`/`handleUpdateTag`/`onDataPacket` (DESCRIPTION), `collectImplicitComponents`/`applyImplicitComponents` (ITEM), `setRemoved`/`clearRemoved` (fragment invalidation), `preRemoveSideEffects` -> fragment `onRemove` (server only, only when the block actually changes).

### Modules and capabilities
`IModule<T>`: a handle identified by a namespaced `Identifier`, optionally backed by a `BlockCapability<T, Direction?>` and/or `ItemCapability<T, ItemAccess>`. Register with `Module.registerModule(id, blockCap[, itemCap])` during mod construction (ids must be unique).

- Internal lookups: `IModuleProvider#getModule(module, side): T?`.
- External lookups: `level.getCapability(cap, pos, side)`. **Call `ModuleCapabilities.registerBlockEntity(event, type)` for every `BlockEntityCore` type** from a `RegisterCapabilitiesEvent` listener on the mod bus.
- `BlockEntityCore#deserialize` calls `invalidateCapabilities()`; call it yourself if a fragment swaps its exposed object at any other time.

### Scoped serialization
`IScopedSerialization` with `NBTSerializationScope` `LEVEL` (world save), `DESCRIPTION` (client sync), `ITEM` (stays with the block's item form through `itszulib:fragment_data`; survival drops need the loot table to `copy_components` it from the block entity). Each fragment writes into its own child keyed by `name()` under `frags`. Use `ValueOutput`/`ValueInput` and codecs, not raw `CompoundTag`.

### Items, storage, transfer adapters
- `IItemStack` wraps `ItemStack`; persisted with `IItemStack.codec()` (overridable in tests via `TestableIItemStack.overrideCodec()`).
- `IItemStorage`: slot-based, default transfer logic, saved as one entry per non-empty slot keyed by index. `IBattery`: double-based energy.
- ItszuLib -> NeoForge: `WrapperResourceHandlerIItemStorage.of(storage)` (per-slot limits, commit-only notifications), `WrapperEnergyHandlerIBattery(battery)` (whole units). Create once per block entity.
- NeoForge -> ItszuLib: `ItemStorageResourceHandler`, `BatteryEnergyHandler`; they open root transactions, so never call them inside one.

### Networks
`INetwork`/`TileNetwork` group `INetworkNode`s (by `Loc4`) into server-side networks in `ItszuLib.NETWORK_MANAGER`, ticked from `ServerTickEvent.Pre/Post`. Nodes are found through the network module on the block entity (`TileNetwork#networkModule`). Chunk unloads drop that chunk's nodes as a batch; block entities must re-add their node when they load. Splits explore iteratively.

### Engine seams for testing
`ILevel`, `IBlockEntity`, `IItemStack` and `Overideable` let logic be unit tested without a game. Tests use `TestableLevel`, `TestableIItemStack`, `TestableLoc4`, `TestableNetwork*`, `TestableFragmentHost`; `MCAssert.failVanillaClass` marks methods tests must not reach.

## Kotlin conventions

- `@Mod` classes are Kotlin `object`s; use `thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS` for the mod event bus and `NeoForge.EVENT_BUS` for game events.
- Registries: `DeferredRegister` in `object`s, registered from the mod object's `init`.
- Use `@JvmField`/`@JvmStatic` on things Java code or reflection needs to see as plain fields/statics.
- Nullability: Kotlin types carry it; prefer non-null returns and `?` only where NeoForge expects a nullable (capability providers).

## Dev content and game tests

`dev/DevContent.kt` registers `itszulib:dev_frag_block` (colorable, 1-slot inventory exposed via `Capabilities.Item.BLOCK`, drops on break) only when `!FMLEnvironment.isProduction()`. `dev/DevGameTests.kt` registers test functions (`Registries.TEST_FUNCTION`) and test instances (`RegisterGameTestsEvent`) on vanilla's 1x1x1 `minecraft:empty` structure. Add a test with `test("name") { helper -> ...; helper.succeed() }`. Current tests: mod loaded, capability/module lookup, level save/load, client update tag, item capability insert with rollback, drops on break, level lookup returns the core, inventory change marks dirty, ITEM-scope component round trip, per-slot resource handler limits. `GameTestHelper#assertValueEqual(expected, actual, name)` takes the expected value first.

## Testing conventions

- Unit tests: `src/test/kotlin`, JUnit 5, names like `Method_ExpectedBehavior` (as in TechnoLich). ModDevGradle puts Minecraft classes on the test classpath, but anything needing registries or a level belongs in a game test.
- Add a game test for anything that crosses into vanilla/NeoForge (registries, block entities, capabilities, serialization with real items, networking).
- Verify with `./gradlew build` **and** `./gradlew runGameTestServer`.
