# Port decisions

Each entry: context, options, trade-offs, recommendation, and status. **BLOCKING** entries shape large amounts of work and wait for the maintainer.

---

## D1. Language: Kotlin with Kotlin for Forge — DECIDED

**Context.** The mod was Scala 2.11 on Forge 1.7.10's built-in Scala language adapter. NeoForge has no Scala adapter for 26.1, and the maintainer ruled Scala out: "if NeoForge supports Kotlin, port to Kotlin; otherwise port to Java."

**Evidence that Kotlin works on NeoForge 26.1.2.112 / Java 25 (checked 2026-09-30):**
- Adapter: **Kotlin for Forge (KFF)**, `thedarkcolour:kotlinforforge-neoforge`, latest **6.3.0** (published 2026-06-28, Maven `https://thedarkcolour.github.io/KotlinForForge/`, branch `6.x`). Its `kffmod` metadata declares `minecraft` `[1.21.9,26.3)`, so 26.1.2 is in range. It bundles Kotlin stdlib/reflect **2.4.0**, coroutines 1.11.0 and serialization 1.11.0 as jar-in-jar.
- Smoke test (scratch project in `%TEMP%\kffprobe`, a NeoForge 26.1.2 build plus the Kotlin JVM plugin 2.4.0 and KFF 6.3.0, `modLoader="kotlinforforge"`): a Kotlin `object` annotated `@Mod` registering a game test through `DeferredRegister` + `RegisterGameTestsEvent`.
  - `./gradlew build runGameTestServer`: BUILD SUCCESSFUL, "All 2 required tests passed" (`kffprobe:kotlin_smoke` + vanilla's built-in one).
  - Negative check: making the Kotlin test call `fail("deliberate")` made `runGameTestServer` exit 1 with `kffprobe:kotlin_smoke failed ... deliberate`. So the Kotlin mod really loads and its tests run.
- The same setup now builds this repo (commit `30ecc56`).

**Decision.** Kotlin 2.4.0 (JVM toolchain 25) with KFF 6.3.0 as a required dependency (`modLoader="kotlinforforge"`, `loaderVersion="[6.3,)"`). Players need the Kotlin for Forge mod installed alongside ItszuLib.

## D2. Source branch: GitLab `develop-1.12.2-types` — DECIDED (changed 2026-09-30)

**First choice (superseded).** The port started from GitHub `develop` (2016-03-20, Minecraft 1.7.10), the newest branch
on GitHub: `develop-refactoring` pointed at the same commit, `develop-customrender`/`develop-network` were older side
branches and `master` only added README edits.

**Why it changed.** The maintainer pointed out that GitHub is a stale mirror and the real history is on GitLab, where
the newest branch is `develop-1.12.2-types` (2020-11-03, Minecraft 1.12.2, ~266 Scala files).

**What that changes.** Not the framework: B1 already replaced the legacy API with a fragment/module framework whose
fragments descend from this same 1.12.2 code's "modules". So:

- `neoforge-26.1` records `gitlab/develop-1.12.2-types` as an ancestor with `git merge -s ours` (e08a911). The tree is
  unchanged; history now shows where the code comes from, and later merges from that branch would be no-ops.
- The 1.12.2 features the framework lacks (fluid storage, multiblocks, menu sync, sided connections, ...) are ported on
  top in the framework's style. The list is [PORTING.md, 1.12.2 features](PORTING.md#1122-features-to-port).

## D3. Mod id, package, version, license — DECIDED (non-blocking)

- Mod id `itszulib` (NeoForge requires lowercase; the 1.7.10 id was `ItszuLib`). Package stays `com.itszuvalex.itszulib`.
- Version `0.2.0` (the last published line was `0.1.0-x`); Maven coordinates `com.itszuvalex.itszulib:itszulib`.
- License `GPL-2.0-or-later`, from the repo's `LICENSE` (GPL v2) and the "version 2 or later" source headers.
- Dropped the dead Artifactory publishing, CircleCI and GitLab CI configs. `maven-publish` to `./repo` stays.

## D4. How mods consume ItszuLib — DECIDED (non-blocking)

The coordinates are `com.itszuvalex.itszulib:itszulib:<version>`. A mod developed alongside a local checkout can use
it as a Gradle composite build (`includeBuild('../ItszuLib')`), which Gradle substitutes for that dependency, so
ItszuLib changes are picked up without publishing. Alternatives: `mavenLocal()` (a publish step after every change)
or a published Maven repo (none exists yet). Under a composite build, ItszuLib's dev-only content and game tests also
load in the consuming mod's dev runs.

## D5. Legacy sources — DECIDED (non-blocking)

Scala sources stay in `src/main/scala` / `src/test/scala` as reference while porting (not compiled, since the Scala plugin is not applied) and are deleted area by area as the Kotlin replacement lands, so `git log` shows each area's old and new form.

## D6. Menus keep vanilla slots — DECIDED (maintainer, 2026-10-01)

1.12.2 dropped vanilla `Slot` syncing in favour of its own `ISync` slots (`SyncItemStorageItemStack`,
`MessageIItemStackSyncClick`). 26.1 menus keep vanilla `Slot`s over `WrapperContainerIItemStorage`: vanilla already
syncs slot contents, handles every click type and drag, and works with recipe viewers. The `ISync` idea is kept for
values that are not slots (power, progress, fluids, side configuration): `MenuCore.addSync` with a `StreamCodec`,
sent by one payload.

Implemented in F5 (`menu/MenuCore`, `MenuSync`, `MenuSyncPayload`). 1.12.2 screens used `SyncItemStorageItemStack`
for every slot; on 26.1 those are plain `StorageSlot`/`OutputSlot`s.

The maintainer confirmed vanilla slots on 2026-10-01. What 1.12.2's synced slots offered over vanilla: clicks went
through the `IItemStorage` API, so storages that return copies from `get` worked, and slots were not tied to the
menu's fixed slot list (scrolling or paged views, slots larger than an item's stack size). The first is now covered on
vanilla slots: `StorageSlot` honours `canInsert` (REVIEW R17) and `MenuCore` writes changed slots back (R18). Paged
views or oversized slots would need a dedicated slot or widget type; nothing uses them yet.

## D7. Menu actions go through one payload to the open menu — DECIDED (non-blocking)

1.12.2 mods had one message per control (`MessageSidedInventoryConfigChange`, `...IOChange` and similar), each naming a block by position and applied by the server without checking that the sender had that block's
menu open or was near it. 26.1 has one `MenuActionPayload(containerId, action, data)`: the server routes it only to the
sender's open `MenuCore` with that container id, and only while `stillValid(player)`; the menu decides what the action
means. Vanilla's `ServerboundContainerButtonClickPacket` does the same for a single int; the payload adds `data`.

## D8. Framework hooks added for F6 and F9 — DECIDED (non-blocking)

Two generic hooks were needed so fragments can react to the world without each block class forwarding calls:

- `IBlockEntityBlockEventHandler.onLoad(level, pos)` and `onNeighborChanged(level, pos)` (default no-ops), forwarded by
  `BlockEntityFragmentCollection`; `BlockEntityCore.onLoad()` (NeoForge's deferred load hook) and
  `EntityBlockCore.neighborChanged` call them. 26.1's `neighborChanged` no longer says which neighbour changed, so the
  hook has no neighbour position.
- `EntityBlockCore.useWithoutItem` opens the `Modules.MENU` host the block entity exposes on the clicked face.

## D9. Wires reconcile their faces on load — DECIDED (non-blocking)

1.12.2 wires trusted their saved connection flags on load and only checked neighbours when placed or when a neighbour
changed. `FragNetworkedWire` checks every face whose neighbour is loaded on load too (faces towards unloaded chunks keep
their flag and are joined when the neighbour loads). This fixes stale connections to blocks removed while the chunk was
unloaded, at the cost of up to six neighbour lookups per wire per load.

## D10. Teams and per-team data — DECIDED (maintainer, 2026-10-01)

Research (and any other per-team data a mod registers) belongs to teams. Every player is always in exactly one team;
a new player gets a solo team they own. Joining is by invite and accept and merges the joiner's data into the team
(research: union); leaving or being removed gives the player a solo team with a copy; disbanding gives every member a
copy. Roles: one owner, who created the team and alone promotes or demotes officers; officers, who invite and remove
anyone but the owner; members. A basic first version, to be refined later.

Default until decided: the owner cannot leave a shared team without handing ownership to another member first.
Invites do not expire.

Persistence deliberately avoids vanilla `SavedData`, whose load path replaces unreadable data with a fresh, empty
instance that is later saved over the file. `TeamStore` decodes strictly, falls back to a backup, refuses to save when
neither file reads, and writes through a verified temporary file and an atomic move (see `team/TeamStore.kt`).

## D11. Multiblocks: controller-less, with home-held state — DECIDED (maintainer, 2026-10-02)

The 1.12.2 port kept multiblock state on a controller block; a part in another chunk lost its state, menu and
capabilities while the controller's chunk was unloaded (REVIEW O3). The maintainer chose controller-less multiblocks
with shared state designed on top, replacing the 1.12.2
system (`MultiBlockInfo`, `IBlockPattern`/`BlockPatternStatic`, `MultiblockStatic`, `FragMultiBlockInfo`,
`FragMultiblockState`, `MultiblockUtils`).

The base model: shapes of offset -> role (`MultiblockShape`), members that save their own membership
(`IMultiblockMember`, `FragMultiblockPart`), formation driven by members loading, a structure id minted once, and break
policies (`DISSOLVE`, `DESTROY_ALL`). On top of it:

- **Home slot.** Every shape has a slot at (0,0,0), its home. A stateful shape's `IMultiblockState` lives on the home
  member and is saved with its chunk. A level-wide store was considered and rejected: it would keep every structure's
  data in memory whenever the level is loaded, so large multiblocks would bloat the whole level (maintainer).
- **Home chunk ticket.** While a stateful structure has a member outside the home chunk whose own chunk ticks, the
  manager keeps the home chunk loaded, not ticking, with an `itszulib:multiblock` ticket (radius 0, 60-tick timeout,
  refreshed every 20 ticks). When the player leaves, members stop ticking, the ticket lapses and the home chunk unloads
  normally. Only ticking chunks refresh it, so two structures cannot keep each other's chunks loaded; a structure
  within one chunk never takes one. A member in another chunk sees no state for the moment the home chunk takes to
  load.
- **Verification.** A member that loads remembering a structure is checked against the home member once the home
  position is loaded; if the home member left (the structure broke or was disbanded while this member was unloaded),
  the member leaves. This closes the gap where a `DISSOLVE` break went unseen by unloaded members, without keeping a record of
  structures.
- **Explicit formation.** `MultiblockManager.form(level, shape, anchor)` forms at a given anchor, and members with
  `autoForm = false` only join that way (for structures built by an item or a process rather than by placing blocks).
  `disband` ends a structure without break effects (no `onBreak`, nothing destroyed), e.g. before replacing its
  blocks.
- **Break hook.** `IMultiblockState.onBreak(level, anchor, brokenAt)` runs once when a member's removal breaks the
  structure, so the state drops its contents.
- **Ticking.** `FragMultiblockTickable` runs the structure once per game tick from whichever member ticks first.
- **Menus and sync.** Membership is synced to clients (DESCRIPTION scope). `FragMenu(..., multiblock = part)` opens on
  any formed member, over the shared state; client side each part keeps a scratch copy of the state for menus to sync
  into.

Dropped: pattern rotation (register one shape per orientation if needed).

## D12. Screens built from components, with side configuration and energy defaults — DECIDED (maintainer, 2026-10-02)

A consumer mod built a 3D side configuration view (the machine and its neighbours, rotated by dragging, faces clicked
to configure, after Ender IO). The maintainer asked for it in ItszuLib, not as a fixed screen but as components that
show or hide behind buttons, with sensible defaults for item, fluid and energy handling (energy through ItszuLib
batteries or NeoForge's energy handler, though no consumer uses it yet).

- **Screens.** `client/screen/ComponentScreen` is an `AbstractContainerScreen` made of `ScreenComponent`s: components
  placed in the image (`addComponent`) and `SidePanel`s toggled by tab buttons on the image's right edge, one open at a
  time (`togglePanel`; widgets are rebuilt so a closed panel's buttons go away). Components keep their state across
  re-inits, may add vanilla widgets, take mouse input before the screen, and draw in the background pass. Clicks on
  the open panel or the tabs do not count as outside the screen. Default look: `ScreenStyle` (plain grey panel, slot
  frames); `extractPanel` replaces it. Default components: `EnergyGauge` (over an `EnergyView`), `FluidGauge`,
  `ProgressBar`, `Label`.
- **3D blocks in a screen.** `client/scene`: `BlockSceneRenderState`/`BlockSceneRenderer`, a NeoForge
  picture-in-picture element drawing blocks from their models plus flat face overlays (registered by
  `client/ItszuLibClient`, on every client); `BlockSceneGeometry` (view space, picking, pure math, unit tested);
  `BlockSceneView` (drag to rotate, press without a drag is a click). Blocks drawn only by block entity renderers show
  as faint boxes.
- **Side configuration.** `MenuCore.enableSideConfig(be, modes)` keeps the `SideConfigMode`s the block entity has
  (default `SideConfigModes.ITEM`, `FLUID`, `ENERGY`, each a configuration module plus a `SideConfigCycler`: `IO`,
  `STORAGE` or `IO_THEN_STORAGE`) and handles `MenuCore.ACTION_SIDE_CONFIG`. ItszuLib's own actions use negative ids,
  routed by `MenuCore.dispatchAction` before `handleAction`. `ComponentScreen` adds `SideConfigPanel` (the 3D view,
  faces shaded by automatic IO, a mode button) as a default panel whenever the menu has side configuration.
- **Energy parity.** `SidedEnergyStorageConfiguration`, `Modules.ENERGY_STORAGE` and `ENERGY_STORAGE_CONFIGURABLE`,
  `FragEnergyStorage` + `addEnergyStorage` (the battery per side and NeoForge's energy capability), `FragEnergyAutoIO`.
  Menus sync energy into an `EnergyView` with `syncEnergy` (a battery) or `syncEnergyHandler` (a NeoForge handler).

The dev machine uses all of it (`DevScreen`: tank and energy gauges, the "IO" tab). Not done: textured styles, layout
helpers beyond fixed positions, and a 3D view of multiblocks (each member shows its own block).

---

## B1. Shape of the ported ItszuLib API — DECIDED: a fragment/module framework (option 2)

**Context.** ItszuLib's 2015–16 API is built around mechanisms that no longer exist (`IInventory`/`ISidedInventory`, `IFluidHandler`, `IExtendedEntityProperties`, block metadata, `SimpleNetworkWrapper`, immediate-mode GL). Mods written against the old API have to be rewritten against whatever shape ItszuLib takes.

**Options.**

1. **Faithful port.** Keep ItszuLib's own API shape (class names, `TileEntityBase`/`TileContainer`, `IItemAccess`/`IItemCollectionAccess`, `@Saveable` reflection, trait mixins such as `TileInventory`/`TileFluidTank`), re-implemented on 26.1 mechanisms. Easiest mapping for old code; `IItemAccess` returning mutable backing stacks does not fit the transactional transfer API.
2. **A fragment/module framework.** Block entities composed of fragments exposing modules on `BlockEntityCore`, `IScopedSerialization` (LEVEL/DESCRIPTION/ITEM), `IItemStack`/`IItemStorage` with transfer-API wrappers, `IBattery`, networks; ItszuLib-specific parts (multiblocks, menus/screens, ...) on top. One coherent modern design; the largest departure from the original.
3. **Hybrid.** Keep the public shape where existing code leans on it (`TileEntityBase`-style bases, `@Saveable` fields, container bases, networks, `Loc4`) with modern implementations underneath (`Loc4` with a dimension `ResourceKey` and codec, `IItemStorage` + `ResourceHandler`/`EnergyHandler` adapters, Value I/O). No fragments.

**Recommendation was** option 3.

**Decision (maintainer, 2026-09-30): option 2.** ItszuLib is rewritten in Kotlin as a fragments/modules framework. The legacy 2016 ItszuLib APIs (`TileEntityBase`/`TileContainer`, `IItemAccess`, `@Saveable` reflection, trait mixins) are not ported.

- **Kotlin idioms:** lookups return nullable `T?`, collections of locations/nodes are `Sequence`s, simple accessors on `Loc4`, `IModule`, `Color` are properties; storage and battery classes are `open`. Registries use ItszuLib's namespace (`Components.FRAGMENT_DATA` = `itszulib:fragment_data`, `Modules.COLORABLE` = `itszulib:colorable`).
- **Details worth knowing:** `Loc4.distSqr` computes in doubles (int arithmetic overflows for coordinates about 46k apart; regression test `DistSqr_FarApartCoordinates_DoesNotOverflow`); `TileNetwork.clear()` also clears its location tracker; `TileNetworkNode` keeps its network in `currentNetwork` (a Kotlin property named `network` clashes with `getNetwork()`).
