# Port decisions

Each entry: context, options, trade-offs, recommendation, and status. **BLOCKING** entries shape large amounts of work and wait for the maintainer.

---

## D1. Language: Kotlin with Kotlin for Forge — DECIDED

**Context.** The mod was Scala 2.11 on Forge 1.7.10's built-in Scala language adapter. NeoForge has no Scala adapter for 26.1, and the maintainer ruled Scala out: "if NeoForge supports Kotlin, port to Kotlin; otherwise port to Java."

**Evidence that Kotlin works on NeoForge 26.1.2.112 / Java 25 (checked 2026-09-30):**
- Adapter: **Kotlin for Forge (KFF)**, `thedarkcolour:kotlinforforge-neoforge`, latest **6.3.0** (published 2026-06-28, Maven `https://thedarkcolour.github.io/KotlinForForge/`, branch `6.x`). Its `kffmod` metadata declares `minecraft` `[1.21.9,26.3)`, so 26.1.2 is in range. It bundles Kotlin stdlib/reflect **2.4.0**, coroutines 1.11.0 and serialization 1.11.0 as jar-in-jar.
- Smoke test (scratch project in `%TEMP%\kffprobe`, technolich's build plus the Kotlin JVM plugin 2.4.0 and KFF 6.3.0, `modLoader="kotlinforforge"`): a Kotlin `object` annotated `@Mod` registering a game test through `DeferredRegister` + `RegisterGameTestsEvent`.
  - `./gradlew build runGameTestServer`: BUILD SUCCESSFUL, "All 2 required tests passed" (`kffprobe:kotlin_smoke` + vanilla's built-in one).
  - Negative check: making the Kotlin test call `fail("deliberate")` made `runGameTestServer` exit 1 with `kffprobe:kotlin_smoke failed ... deliberate`. So the Kotlin mod really loads and its tests run.
- The same setup now builds this repo (commit `30ecc56`).

**Decision.** Kotlin 2.4.0 (JVM toolchain 25) with KFF 6.3.0 as a required dependency (`modLoader="kotlinforforge"`, `loaderVersion="[6.3,)"`). Players need the Kotlin for Forge mod installed alongside ItszuLib.

## D2. Source branch: GitLab `develop-1.12.2-types` — DECIDED (changed 2026-09-30)

**First choice (superseded).** The port started from GitHub `develop` (2016-03-20, Minecraft 1.7.10), the newest branch
on GitHub: `develop-refactoring` pointed at the same commit, `develop-customrender`/`develop-network` were older side
branches and `master` only added README edits.

**Why it changed.** The maintainer pointed out that GitHub is a stale mirror and the real history is on GitLab, where
the newest branch is `develop-1.12.2-types` (2020-11-03, Minecraft 1.12.2, ~266 Scala files). Femtocraft's newest
branch (`develop-1.12.2-v3`) builds against it.

**What that changes.** Not the framework: B1 already replaced the legacy API with the Kotlin version of TechnoLich's
framework, and TechnoLich descends from this same 1.12.2 code (its "modules" are the framework's fragments). So:

- `neoforge-26.1` records `gitlab/develop-1.12.2-types` as an ancestor with `git merge -s ours` (e08a911). The tree is
  unchanged; history now shows where the code comes from, and later merges from that branch would be no-ops.
- The 1.12.2 features the framework lacks (fluid storage, multiblocks, menu sync, sided connections, ...) are ported on
  top in the framework's style. The list is [PORTING.md, 1.12.2 features](PORTING.md#1122-features-to-port).
- Changes to shared framework code (fragments, storage, networks, `Loc4`, wrappers) are reported so TechnoLich gets them.

## D3. Mod id, package, version, license — DECIDED (non-blocking)

- Mod id `itszulib` (NeoForge requires lowercase; the 1.7.10 id was `ItszuLib`). Package stays `com.itszuvalex.itszulib`.
- Version `0.2.0` (the last published line was `0.1.0-x`); Maven coordinates `com.itszuvalex.itszulib:itszulib`.
- License `GPL-2.0-or-later`, from the repo's `LICENSE` (GPL v2) and the "version 2 or later" source headers.
- Dropped the dead Artifactory publishing, CircleCI and GitLab CI configs. `maven-publish` to `./repo` stays as in technolich.

## D4. How Femtocraft consumes ItszuLib — DECIDED (non-blocking)

Gradle composite build: Femtocraft's `settings.gradle` does `includeBuild('../ItszuLib')` (overridable with `-Pitszulib_dir=...`), and depends on `com.itszuvalex.itszulib:itszulib:${itszulib_version}`, which Gradle substitutes with this project. In dev runs FML loads ItszuLib from `build/libs/itszulib-<version>.jar`. Alternatives: `mavenLocal()` (needs a publish step after every ItszuLib change) or a published Maven repo (none exists). Composite builds keep both repos editable together without publishing.

Consequence: ItszuLib's dev-only content and game tests also load in Femtocraft's dev runs (they run in Femtocraft's `runGameTestServer` too). That is harmless and catches library regressions.

## D5. Legacy sources — DECIDED (non-blocking)

Scala sources stay in `src/main/scala` / `src/test/scala` as reference while porting (not compiled, since the Scala plugin is not applied) and are deleted area by area as the Kotlin replacement lands, so `git log` shows each area's old and new form.

## D6. Menus keep vanilla slots — DECIDED, AWAITING MAINTAINER SIGN-OFF

1.12.2 dropped vanilla `Slot` syncing in favour of its own `ISync` slots (`SyncItemStorageItemStack`,
`MessageIItemStackSyncClick`). 26.1 menus keep vanilla `Slot`s over `WrapperContainerIItemStorage`: vanilla already
syncs slot contents, handles every click type and drag, and works with recipe viewers. The `ISync` idea is kept for
values that are not slots (power, progress, fluids, side configuration): `MenuCore.addSync` with a `StreamCodec`,
sent by one payload.

Implemented in F5 (`menu/MenuCore`, `MenuSync`, `MenuSyncPayload`). Femtocraft's v3 screens used `SyncItemStorageItemStack`
for every slot; on 26.1 those are plain `StorageSlot`/`OutputSlot`s. The maintainer has not yet confirmed that losing
1.12.2's synced-slot click handling (`MessageIItemStackSyncClick`) is acceptable; until then this is the default.

## D7. Menu actions go through one payload to the open menu — DECIDED (non-blocking)

Femtocraft 1.12.2 had one message per control (`MessageSidedInventoryConfigChange`, `...IOChange`, fluid and nanite
variants), each naming a block by position and applied by the server without checking that the sender had that block's
menu open or was near it. 26.1 has one `MenuActionPayload(containerId, action, data)`: the server routes it only to the
sender's open `MenuCore` with that container id, and only while `stillValid(player)`; the menu decides what the action
means. Vanilla's `ServerboundContainerButtonClickPacket` does the same for a single int; the payload adds `data`.

## D8. Framework hooks added for F6 and F9 — DECIDED (non-blocking), mirror into TechnoLich

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

---

## B1. Shape of the ported ItszuLib API — DECIDED: adopt TechnoLich's framework (option 2)

**Context.** ItszuLib's 2015–16 API is built around mechanisms that no longer exist (`IInventory`/`ISidedInventory`, `IFluidHandler`, `IExtendedEntityProperties`, block metadata, `SimpleNetworkWrapper`, immediate-mode GL). The same author's later mod, **technolich** (already ported to 26.1.2), re-designed several of the same concepts and is visibly descended from this code: `Loc4`, `OverridableFunction`→`Overideable`, `IItemStorage`, `LocationTracker`, `INetwork`/`TileNetwork`/`ManagerNetwork`→`NetworkManager`. Femtocraft (~17k lines) is written against ItszuLib's API, so whatever shape ItszuLib takes, Femtocraft is rewritten against it.

**Options.**

1. **Faithful port.** Keep ItszuLib's own API shape (class names, `TileEntityBase`/`TileContainer`, `IItemAccess`/`IItemCollectionAccess`, `@Saveable` reflection, trait mixins such as `TileInventory`/`TileFluidTank`), re-implemented on 26.1 mechanisms.
   - Pro: the easiest mapping for Femtocraft's code; preserves the original design for review.
   - Con: re-implements designs the author already replaced in technolich. `IItemAccess` returning mutable backing stacks does not fit the transactional transfer API, so adapters add a second layer anyway.
2. **Adopt technolich's framework.** Re-express technolich's framework in Kotlin as ItszuLib's core: fragments + modules on `BlockEntityCore`, `IScopedSerialization` (LEVEL/DESCRIPTION/ITEM), `IItemStack`/`IItemStorage` with transfer-API wrappers, `IBattery`, networks. Port only ItszuLib-specific parts (multiblocks, menus/screens and GUI widgets, pathfinding, config, player data) on top. Femtocraft tiles become fragment compositions.
   - Pro: one coherent, modern design that is already proven on 26.1.2 (with game tests); the review baseline is the author's own later thinking.
   - Con: largest departure from the original; Femtocraft's port becomes a redesign instead of a translation. Two copies of the framework (Java in technolich, Kotlin here) drift unless one later depends on the other.
3. **Hybrid (recommended).** Keep ItszuLib's public shape where Femtocraft leans on it (`TileEntityBase`-style base block entity and block, `@Saveable` fields, multiblock protocol, container/menu bases, networks, `Loc4`), but take technolich's already-debugged 26.1 implementations wherever the same concept exists: `Loc4` (dimension `ResourceKey`, codec), `LocationTracker`, `INetwork`/`TileNetwork`/`NetworkManager`, `IItemStorage` + `ResourceHandler`/`EnergyHandler` adapters, Value I/O serialization. Replace the `IItemAccess` family with `IItemStorage` (its ScalaTest behaviour is re-checked as JUnit tests). No fragments/modules.
   - Pro: Femtocraft code translates fairly directly; overlapping pieces reuse proven code; smaller scope than option 2.
   - Con: two base-class styles across the author's mods (inheritance/trait mixins here, fragments in technolich).
4. **Merge the codebases** (make technolich's framework the shared library, or make ItszuLib depend on technolich). Needs changes to technolich, which is out of scope for this job; listed for completeness.

**Sub-decision inside all options:** `@Saveable` reflection. Keep it (annotate Kotlin backing fields with `@field:Saveable`, serialize through `ValueOutput`/codecs by field type) vs. explicit `save`/`load` overrides. Recommendation: keep it for option 1 or 3, since every Femtocraft tile uses it and it maps cleanly to Value I/O; it is replaced by fragments' own serialization in option 2.

**Recommendation was** option 3.

**Decision (maintainer, 2026-09-30): option 2.** ItszuLib is rewritten in Kotlin as TechnoLich's fragments/modules framework, and Femtocraft is rebuilt on fragments. The legacy 2016 ItszuLib APIs (`TileEntityBase`/`TileContainer`, `IItemAccess`, `@Saveable` reflection, trait mixins) are not ported.

- **Source of truth:** `F:\Projects\technolich`, branch `neoforge-26.1`. The Kotlin port was synced to **technolich@f021246** in ItszuLib **76501a4** (stable pieces first in 00594ed). Concepts, names, save keys and serialization formats are unchanged; unit and game tests were ported alongside.
- **One Kotlin framework:** TechnoLich is itself being moved to Kotlin, starting from a copy of ItszuLib 76501a4 with the package renamed. Fixes found in either repo should be applied to both.
- **Kotlin idioms used:** `Optional<T>` returns became nullable `T?`; `Stream` became `Sequence`; simple accessors on `Loc4`, `IModule`, `Color` became properties; storage and battery classes are `open` like their Java originals. Registries (`Components.FRAGMENT_DATA` = `itszulib:fragment_data`, `Modules.COLORABLE` = `itszulib:colorable`) use ItszuLib's namespace.
- **Deliberate differences from the Java:** `Loc4.distSqr` computes in doubles (the Java int arithmetic overflowed for coordinates about 46k apart; regression test `DistSqr_FarApartCoordinates_DoesNotOverflow`); `TileNetwork.clear()` also clears its location tracker; `TileNetworkNode` keeps its network in `currentNetwork` (a Kotlin property named `network` clashes with `getNetwork()`).
- ItszuLib-specific parts that are not in TechnoLich (multiblock helpers, menu/screen bases) are added on top only as Femtocraft needs them.

## B2. Femtocraft scope — DECIDED: gameplay logic + simple models (option 2)

See `../Femtocraft/docs/DECISIONS.md` B2.
