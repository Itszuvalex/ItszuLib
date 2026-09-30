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

## D2. Source branch: `develop` — DECIDED

`develop` and `develop-refactoring` point at the same newest commit (2016-03-20). `develop-customrender` (WIP OBJ/VBO loader, 3 commits) and `develop-network` (1 cleanup commit, 51 behind) are older side branches; their content is either obsolete on modern Minecraft or superseded. `master` only adds README edits. See [PORTING.md](PORTING.md#source-branch).

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

---

## B1. BLOCKING — Shape of the ported ItszuLib API

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

**Recommendation:** option 3.
