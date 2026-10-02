# AGENTS.md

Guidance for coding agents (Claude Code, Codex, etc.) and humans working in this repository.

## What this is

ItszuLib is Itszuvalex's shared library mod: a Kotlin **block entity framework** of block entities composed of fragments, a module/capability layer, scoped serialization, item/fluid/energy storage abstractions with NeoForge transfer-API adapters, block entity networks, multiblocks, menus and screens. It is a standalone framework: it knows nothing about the mods built on it (see [docs/DECISIONS.md](docs/DECISIONS.md) B1).

- **Minecraft 26.1.2 / NeoForge 26.1.2.112 / Java 25**, ModDevGradle (`net.neoforged.moddev` 2.0.148), Gradle 9.2.1.
- Written in **Kotlin 2.4.0**, loaded through **Kotlin for Forge 6.3.0** (`thedarkcolour:kotlinforforge-neoforge`, `modLoader="kotlinforforge"`). KFF is a required runtime mod: it provides the language loader and the Kotlin stdlib, reflect, coroutines and serialization. Do not add a second copy of the stdlib (`kotlin.stdlib.default.dependency=false`).
- Mod id `itszulib`, package `com.itszuvalex.itszulib`, GPL-2.0-or-later.
- Ported from Forge 1.7.10 / Scala 2.11 on branch `neoforge-26.1` (from `develop`), merged into `main`, the default branch. `develop` and the other `develop-*` branches hold the 1.12.2 and older code. Port status: [docs/PORTING.md](docs/PORTING.md). Decisions and open questions: [docs/DECISIONS.md](docs/DECISIONS.md). Review findings (fixed and open): [docs/REVIEW.md](docs/REVIEW.md).

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

On NixOS, run Gradle inside the repo's dev shell (`flake.nix`): `nix develop`, or direnv with a local `.envrc` containing `use flake` (`.envrc` is not committed). It puts JDK 25 on `PATH`/`JAVA_HOME` and the native libraries the dev client loads on `LD_LIBRARY_PATH`. Without it, `runClient` fails with `GLX: Failed to load GLX`, because NixOS keeps the GPU drivers in `/run/opengl-driver/lib` with no GL dispatcher on the loader path. Other platforms ignore the flake.

## Documentation

NeoForge's API changes a lot between versions and many online examples are stale. Prefer these sources, in order:

1. **Decompiled, NeoForge-patched Minecraft sources**: `build/moddev/artifacts/minecraft-patched-<version>-sources.jar` (created by any Gradle build). NeoForge classes: `neoforge-<version>-universal.jar` under `$GRADLE_USER_HOME/caches/modules-2/files-2.1/net.neoforged/neoforge/`; use `javap` to check signatures.
2. **NeoForge docs**: https://docs.neoforged.net/docs/ (unversioned pages are 26.1): capabilities, transactions, Value I/O, block entities, menus, networking, game tests.
3. **Porting primers**: https://docs.neoforged.net/primer/docs/ (per-version change lists).
4. **ModDevGradle docs**: https://docs.neoforged.net/toolchain/docs/plugins/mdg/.
5. **Kotlin for Forge**: https://github.com/thedarkcolour/KotlinForForge (branch `6.x` for 1.21.9–26.2).
6. **Forge 1.7.10 era** (historical, for the legacy code's intent only).

## Source layout

```
src/main/kotlin/com/itszuvalex/itszulib/
├── ItszuLib.kt            @Mod object (KFF): module init, data components, payload registration, dev content
│                          (non-production only), server tick / chunk unload / server stop -> NETWORK_MANAGER,
│                          MultiblockManager.SERVER, multiblock ticket type
├── api/
│   ├── Api.kt             Capabilities (COLORABLE), Modules (COLORABLE, ITEM/FLUID_STORAGE, *_STORAGE_CONFIGURABLE,
│   │                      MULTIBLOCK_MEMBER, MENU; Modules.init()), Components (FRAGMENT_DATA = itszulib:fragment_data),
│   │                      ModuleCapabilities (registers a BlockEntityCore type's modules + STANDARD NeoForge caps)
│   ├── adapters/          Engine-facing interfaces: IModule/Module, IModuleProvider, IBlockEntity, ILevel,
│   │                      IItemStack, IFluidStack, IBattery, IColorable
│   ├── multiblock/        MultiblockShape, IMultiblockMember, IMultiblockState, MultiblockManager (+ instance,
│   │                      chunk tickets: MultiblockTickets.kt), Multiblock{Item,Fluid}StorageConfiguration
│   ├── storage/           IItemStorage and IFluidStorage + implementations (Array, Slice, Aggregate, NBT, Dynamic,
│   │                      ResourceHandler-backed); IBattery implementations
│   ├── utility/           Loc4 (+Level/ILevel/Indirect), ChunkCoord, LocationTracker, DirectionUtil, module
│   │                      capability maps, IScopedSerialization + NBTSerializationScope, Overideable, sided holders
│   └── wrappers/          Vanilla/NeoForge <-> ItszuLib adapters (WrapperLevel, WrapperBlockEntity,
│                          WrapperVanillaItemStack/FluidStack, WrapperContainerIItemStorage,
│                          WrapperResourceHandlerIItemStorage/IFluidStorage, WrapperEnergyHandlerIBattery, WrapperCache)
├── client/                ScreenHelpers (fluid tanks, progress bars, tooltips) + ScreenMath; ItszuLibClient (client
│   │                      registrations); screen/ (ComponentScreen, ScreenComponent, SidePanel, ScreenStyle, gauges,
│   │                      SideConfigPanel, TechTreeView, ThemedButton, ScreenTheme/ScreenThemes, ScreenGrain, ScreenThemeConfig); scene/ (BlockScene*: 3D blocks in a screen). Client only.
│   │                      DECISIONS D12
├── core/                  BlockEntityCore, TickableBlockEntityCore, EntityBlockCore, TickableEntityBlockCore,
│   │                      HorizontalFacing (+ Horizontal/TickableHorizontal block cores), fragment interfaces
│   │                      (Fragments.kt), networks (Networks.kt), producer/consumer distribution (Distribution.kt),
│   │                      Sided{Item,Fluid}StorageConfiguration
│   └── frag/              Fragment base classes; FragColorable, FragDropInventory; storage fragments
│                          (FragItem/Fluid/EnergyStorage, FragSidedConfiguration, FragItem/Fluid/EnergyAutoIO); multiblock fragments
│                          (FragMultiblockPart, FragMultiblockTickable); FragMenu;
│                          FragConnectable, FragNetworkedWire
├── menu/                  MenuCore (slots, shift-click, syncs, syncEnergy/EnergyView, enableSideConfig), MenuSync/MenuSyncs,
│                          MenuSyncPayload, MenuActionPayload, MenuSideConfig (SideConfigMode/Modes/Cyclers),
│                          IMenuHost, BlockMenus
├── network/               PacketHandler, ItszuLibNetwork (registers ItszuLib's payloads)
├── store/                 Crash-safe server data: SafeStore + StoreFormat (file), StoreManager (state, change, save),
│                          ServerStores (server lifecycle)
├── research/              Tech trees: Technology (datapack registry itszulib:technology), Technologies (rules),
│                          TechTree (registry, team progress and queue, gating, TechnologyResearchedEvent), TechTreeLayout
├── team/                  Teams and per-team data: Team/TeamState (rules + invariants), TeamDataType + Research,
│                          TeamCodec, TeamStore (file persistence), TeamManager, TeamNetwork (sync + lifecycle),
│                          TeamCommands (/itszulib team, /itszulib research unlock|progress|queue|unqueue)
├── compat/jei/            Optional JEI plugin: keeps JEI's overlays clear of ComponentScreen side panels (loads only
│                          with JEI)
├── dev/                   Dev-only blocks, menu, screen and game tests (never registered in production)
└── util/                  Color, InventoryUtils (item dropping), StorageUtils (item counting/removal), FaceBitSet, Task,
                           Singleton, DevEnvironmentCondition (load condition itszulib:dev_environment)
src/main/resources/        assets/itszulib/lang/en_us.json (screen helper strings), data/itszulib/structure/dev_5x3x5.nbt,
                           data/itszulib/itszulib/technology/dev_*.json (dev-only test technologies)
src/test/kotlin/...        JUnit tests + Testable* fakes that avoid vanilla objects (TestHelpers.kt, CoreTests.kt)
```

The 1.7.10 Scala sources were removed on this branch (the framework replaced them); they remain on `develop`.

## Core concepts

Kotlin conventions in the API: lookups return nullable `T?`, collections of locations/nodes are `Sequence`s, and `Loc4.x/y/z/pos/dimensionId`, `IModule.id/blockCapability/itemCapability` and `Color.alpha/red/green/blue` are properties.

### Fragments and BlockEntityCore
A `BlockEntityCore` owns a `BlockEntityFragmentCollection` (`fragList`). Compose behaviour in the block entity's `init`:

- `fragList.addFragment(IBlockEntityFragment<T>)`: a fragment that exposes a module `T` (e.g. `FragColorable` exposes itself as `IColorable`). `faceToModuleMapper(be)` maps a nullable `Direction` to the live instance (or null). Expose an interface whose mutators save/sync, never a mutable value object.
- `fragList.addInternalFragment(IInternalBlockEntityFragment)`: hooks without an exposed module (serialization, `onRemove`, e.g. `FragDropInventory`).
- Fragment `name()`s must be unique per block entity (they key saved data) and each module may be exposed by one fragment only; both throw `IllegalArgumentException`.
- Fragments get an `IFragmentHost` in `onAttach`. `InternalBlockEntityFragment`/`BlockEntityFragment` subclasses call `markDirty()` after changing saved state and `markDirtyAndSync()` after changing client-visible (DESCRIPTION) state.
- Storages report their own changes: pass an `onChanged` runnable (e.g. `{ markDirty() }`) to `ItemStorageArray`/`ItemStorageNBT`/`PowerBattery`. `setSlotQuietly`/`setStorageQuietly` skip it; the NeoForge adapters use them inside transactions and call `setChanged()` once on root commit.
- `fragList.addCapability(cap) { side -> ... }` exposes a non-module capability, typically one of `ModuleCapabilities.STANDARD` (NeoForge item/fluid/energy).
- `fragList.addTickable(...)`: ticked by `TickableBlockEntityCore` when the block's `TickableEntityBlockCore#hasTicker(side)` returns true.

`BlockEntityCore` wires fragments into the vanilla lifecycle: `saveAdditional`/`loadAdditional` (LEVEL), `getUpdateTag`/`handleUpdateTag`/`onDataPacket` (DESCRIPTION), `collectImplicitComponents`/`applyImplicitComponents` (ITEM), `setRemoved`/`clearRemoved` (fragment invalidation), `preRemoveSideEffects` -> fragment `onRemove` (server only, only when the block actually changes), `onLoad`/`onChunkUnloaded` -> fragment `onLoad`/`onChunkUnloaded`.

### Modules and capabilities
`IModule<T>`: a handle identified by a namespaced `Identifier`, optionally backed by a `BlockCapability<T, Direction?>` and/or `ItemCapability<T, ItemAccess>`. Register with `Module.registerModule(id, blockCap[, itemCap])` during mod construction (ids must be unique).

- Internal lookups: `IModuleProvider#getModule(module, side): T?`.
- External lookups: `level.getCapability(cap, pos, side)`. **Call `ModuleCapabilities.registerBlockEntity(event, type)` for every `BlockEntityCore` type** from a `RegisterCapabilitiesEvent` listener on the mod bus.
- `BlockEntityCore#deserialize` calls `invalidateCapabilities()`; call it yourself if a fragment swaps its exposed object at any other time.

### Scoped serialization
`IScopedSerialization` with `NBTSerializationScope` `LEVEL` (world save), `DESCRIPTION` (client sync), `ITEM` (stays with the block's item form through `itszulib:fragment_data`; survival drops need the loot table to `copy_components` it from the block entity). Each fragment writes into its own child keyed by `name()` under `frags`. Use `ValueOutput`/`ValueInput` and codecs, not raw `CompoundTag`.

### Items, storage, transfer adapters
- `IItemStorage.insert` honours `canInsert` (a refused stack comes back whole); a storage's owner filling slots that refuse outside insertion (outputs) uses `insertUnchecked`. Slices and aggregates forward `canInsert`/`maxStackSize`.
- `IItemStack` wraps `ItemStack`; persisted with `IItemStack.codec()` (overridable in tests via `TestableIItemStack.overrideCodec()`).
- `IItemStorage`: slot-based, default transfer logic, saved as one entry per non-empty slot keyed by index. `IBattery`: double-based energy.
- ItszuLib -> NeoForge: `WrapperResourceHandlerIItemStorage.of(storage)` (per-slot limits, commit-only notifications), `WrapperEnergyHandlerIBattery(battery)` (whole units). Create once per block entity.
- NeoForge -> ItszuLib: `ItemStorageResourceHandler`, `BatteryEnergyHandler`; their writes join an open transaction (`Transactions.openJoined`), so inside one they commit or roll back with it.

### Multiblocks
Controller-less, with shared state (DECISIONS D11). `MultiblockShape.register(id, slots, breakPolicy, state)` maps offsets to role names; the (0,0,0) slot is required and is the structure's home. A member exposes `IMultiblockMember` (usually `FragMultiblockPart(candidateRoles, autoForm)` through `Modules.MULTIBLOCK_MEMBER`) and saves its own membership. `MultiblockManager.SERVER` forms structures when auto-forming members load (all slots loaded, roles matching) or on `form(level, shape, anchor)`, and `disband`s them without break effects. Breaking a member breaks the structure: `DISSOLVE` frees the others, `DESTROY_ALL` destroys them (loading their chunks). A stateful shape's `IMultiblockState` lives on the home member, saved with its chunk; other members reach it with `FragMultiblockPart.sharedState()`, and it gets `onBreak` to drop its contents. While a member outside the home chunk is in a ticking chunk, the manager keeps the home chunk loaded without ticking it (an `itszulib:multiblock` ticket that lapses 60 ticks after the last refresh). Reloaded members are checked against the home member and leave if their structure is gone. `FragMultiblockTickable` runs once per game tick per structure. Faces between members: `Multiblock{Item,Fluid}StorageConfiguration`. `ItszuLib` ticks the manager (`ServerTickEvent.Post`) and clears it on server stop.

### Screens
Build machine screens on `ComponentScreen` (DECISIONS D12): place components in `addComponents()` (`EnergyGauge`,
`FluidGauge`, `ProgressBar`, `Label` or your own `ScreenComponent`), add `SidePanel`s for content behind a tab. A menu
that calls `enableSideConfig(blockEntity[, modes])` gets the 3D side configuration panel automatically (for a formed
multiblock member it shows and configures the whole structure); modes default
to item, fluid and energy (`SideConfigModes`), and a mode's `SideConfigCycler` decides what a click changes. Sync
energy with `syncEnergy { battery }` or `syncEnergyHandler { handler }`. ItszuLib's own menu actions use negative ids
(`MenuCore.ACTION_SIDE_CONFIG`); give yours non-negative ids and handle them in `handleAction`. Screens draw in a theme
(DECISIONS D16): `ScreenThemes` holds `itszulib:light`, `itszulib:dark` and JSON themes from
`assets/<ns>/itszulib/themes/*.json` (`parent` plus any `colors`); a screen picks its default with `defaultTheme()`,
the client config (`ScreenThemeConfig`) can force one and turn grain off, and components read colours from
`ScreenStyle` (pointed at the screen's theme while it draws); use `ThemedButton` rather than vanilla buttons so
buttons follow the theme too. Slots are drawn as insets, take-only slots ringed and
hints (`addStorageSlots(..., hint = stack)`, `SlotLook`) faded into empty slots. Anything a screen
draws outside its image belongs in `ComponentScreen.extraAreas()`, which the JEI plugin reports so JEI's overlays stay
clear. JEI is a `compileOnly` dependency plus a dev-run `localRuntime` (`-Pjei=false` leaves it out).

### Networks
`INetwork`/`TileNetwork` group `INetworkNode`s (by `Loc4`) into server-side networks in `ItszuLib.NETWORK_MANAGER`, ticked from `ServerTickEvent.Pre/Post`. Nodes are found through the network module on the block entity (`TileNetwork#networkModule`). Chunk unloads drop that chunk's nodes as a batch; block entities must re-add their node when they load. Splits explore iteratively.

A `DistributingTileNetwork` also moves an amount between producers, storage and consumers every tick (DECISIONS D15): its `IDistributionNode` nodes return `DistributionParticipant(key, role, Distributable)`s (each key once), and `DistributionAlgorithm` moves within each participant's transfer limit. `DistributableBattery` adapts an `IBattery`; the algorithm can also be run directly on lists of `Distributable`s.

### Teams and per-team data
Every player is always in exactly one team (a new player gets a solo team they own). Mods register per-team data with `TeamDataTypes.register(TeamDataType(id, codec, empty, merge, copy))` during mod construction; ItszuLib registers `Research.TYPE` (`itszulib:research`: unlocked ids, merged by union, plus partial progress per technology, merged by maximum). Joining (invite and accept) merges the joiner's data into the team; leaving or being removed gives the player a solo team with a copy; disbanding gives every member a copy. Roles: owner (promote/demote officers, hand over ownership, rename, disband; cannot leave a shared team without handing it over), officers (invite, revoke, remove anyone but the owner), members. Read `ItszuLib.TEAMS.state`; change only through `ItszuLib.TEAMS.change { state -> newState }` on the server thread. After a change moves players between teams, a `TeamMembershipChangedEvent` (player, `from` team or null for a new player, `to` team, data already merged or copied) is posted per moved player on the game bus; `TeamState.membershipChanges(old, new)` is the same diff, pure.

Data integrity (do not weaken): `TeamState` is immutable and checks its invariants on construction, so an operation yields a valid state or changes nothing. Persistence is `TeamStore` (a `SafeStore`, below), not vanilla `SavedData` (vanilla replaces unreadable saved data with a fresh empty instance and later saves it over the file): strict decoding, fallback to `teams.dat.bak` with the bad file moved aside, refusal to save for the session if neither file reads, and temp-file + read-back + atomic-move saves. Unregistered data types are kept raw. Stored at `<world>/data/itszulib/teams.dat`; clients get their own team through `TeamSyncPayload` (only connections that negotiated it). See DECISIONS D10.

### Server data stores
For server data that must not be lost, use `store/` rather than `SavedData` (DECISIONS D14): an immutable value, a `StoreFormat<T>` (empty, encode, strict decode that throws on anything unreadable), a `StoreManager<T>` changed only through `change { old -> new }` on the server thread, and `ServerStores.register(Identifier(ns, "file.dat"), manager, format)` to load it at server start (`<world>/data/<ns>/file.dat`), save it with the overworld and unload it at stop. `SafeStore` keeps a backup, moves an unreadable file aside, refuses to save over data it could not read, and writes through a verified temp file and an atomic move.

### Tech trees
Technologies are a synced datapack registry, `itszulib:technology` (`data/<ns>/itszulib/technology/<path>.json`: `tree`, `prerequisites`, `cost`, `icon`, optional `name`, `description`, `position`, `hidden`, `unlocked_by_default`). Research is per team (`Research` team data). `TechTree.of(registryAccess)` gives the `Technologies` (rules: `state` is RESEARCHED, AVAILABLE when every prerequisite is researched, LOCKED, or HIDDEN; `problems()`; `layout(tree)`). Mods produce progress and call `TechTree.addProgress(server, team, id, amount)` (returns what it used; unlocks at the cost and posts `TechnologyResearchedEvent`) in batches, since each change syncs the team; gate with `TechTree.isResearched(player, id)` on either side. Teams keep a research queue (`Research.queue`): `TechTree.queue(server, team, id)` appends the technology after its missing prerequisites, `unqueue` removes it with what needs it, and `TechTree.focus(server, team, tree)` is the first queued technology of a tree that is available, for machines that research whatever the team chose. Draw a tree with `TechTreeView` in a `ComponentScreen` (queue badges; `onSelect` on click, `onAlternate` on right-click). Data meant only for dev runs takes `"neoforge:conditions": [{"type": "itszulib:dev_environment"}]`. See DECISIONS D13.

### Engine seams for testing
`ILevel`, `IBlockEntity`, `IItemStack` and `Overideable` let logic be unit tested without a game. Tests use `TestableLevel`, `TestableIItemStack`, `TestableLoc4`, `TestableNetwork*`, `TestableFragmentHost`; `MCAssert.failVanillaClass` marks methods tests must not reach.

## Kotlin conventions

- `@Mod` classes are Kotlin `object`s; use `thedarkcolour.kotlinforforge.neoforge.forge.MOD_BUS` for the mod event bus and `NeoForge.EVENT_BUS` for game events.
- Registries: `DeferredRegister` in `object`s, registered from the mod object's `init`.
- Use `@JvmField`/`@JvmStatic` on things Java code or reflection needs to see as plain fields/statics.
- Nullability: Kotlin types carry it; prefer non-null returns and `?` only where NeoForge expects a nullable (capability providers).

## Consumers

Keep this repo independent of any mod built on it: no mod-specific code, names or docs here. Mods consume it as
`com.itszuvalex.itszulib:itszulib` (DECISIONS D4), often as a Gradle composite build of a sibling checkout. Keep
framework changes generic.

## Dev content and game tests

`dev/DevContent.kt` registers dev blocks only when `!FMLEnvironment.isProduction()`:

- `itszulib:dev_frag_block`: colorable, 1-slot inventory exposed via `Capabilities.Item.BLOCK`, drops on break.
- `itszulib:dev_machine` (`dev/DevStorageContent.kt`): 2-slot inventory (slot 0 "input", slot 1 "output"), a 4000 mB tank and a 10000 battery, each behind a sided configuration (`FragSidedConfiguration`), exposed per side by `FragItemStorage`/`FragFluidStorage`/`FragEnergyStorage`, with item, fluid and energy auto IO every tick. Its screen (`DevScreen`) is a `ComponentScreen` with tank and energy gauges and the side configuration panel.
- `itszulib:dev_multiblock`: a part of a two-block multiblock (`DevMultiblockBlock.SHAPE`, `itszulib:dev_pair`: a `core` and a `wing` east of it); two side by side form on their own. `FragMultiblockPart`, a shared `DevCounter` tick counter, a one-slot inventory and a `MultiblockSidedItemStorageConfiguration`.

`dev/DevGameTests.kt` registers test functions (`Registries.TEST_FUNCTION`) and test instances (`RegisterGameTestsEvent`). Add a test with `test("name") { helper -> ...; helper.succeed() }` (vanilla's 1x1x1 `minecraft:empty` structure) or `test("name", DevGameTests.EMPTY_5X3X5) { ... }` for tests that need neighbours (`data/itszulib/structure/dev_5x3x5.nbt`, an empty 5x3x5 structure). Framework tests live in `DevGameTests`; storage, sided configuration, auto IO and multiblock tests in `DevStorageGameTests`; menu tests in `DevMenuGameTests`; tech tree tests in `DevResearchGameTests`. `GameTestHelper#assertValueEqual(actual, expected, name)` takes the actual value first.

## Testing conventions

- Unit tests: `src/test/kotlin`, JUnit 5, names like `Method_ExpectedBehavior`. ModDevGradle puts Minecraft classes on the test classpath, but anything needing registries or a level belongs in a game test.
- Add a game test for anything that crosses into vanilla/NeoForge (registries, block entities, capabilities, serialization with real items, networking).
- Verify with `./gradlew build` **and** `./gradlew runGameTestServer`.
