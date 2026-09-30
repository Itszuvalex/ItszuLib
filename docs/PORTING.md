# Porting ItszuLib to NeoForge 26.1.2

Status log and inventory for the port on branch `neoforge-26.1`. Decisions live in [DECISIONS.md](DECISIONS.md).

## Source branch

Ported from `develop` (tip `2016-03-20 Add .gitlab-ci.yml`).

| Branch | Last commit | Relation to `develop` | Used? |
|---|---|---|---|
| `develop` | 2016-03-20 | — | **Yes** |
| `develop-refactoring` | 2016-03-20 | identical to `develop` (0 ahead / 0 behind) | same commit |
| `develop-customrender` | 2016-03-02 | 3 ahead, 26 behind: a WIP VBO/OBJ loader ("no luck yet really") | No: modern MC has its own model pipeline (NeoForge OBJ loader) |
| `develop-network` | 2015-09-25 | 1 ahead ("TileNetwork cleanup"), 51 behind | No: superseded by later `develop` network code |
| `master` | 2021-04-26 | 2 ahead (README edits only), 30 behind | No |

Every ItszuLib symbol Femtocraft (`develop-gui`) imports exists on `develop`, so the two branches are consistent.

## What the original targets

- Minecraft **1.7.10**, Forge `10.13.4.1448`, ForgeGradle 1.2, Java 7.
- Scala 2.11.8 (`modLanguage = "scala"`), ScalaTest/ScalaMock tests; a few Java files.
- Published to a private Artifactory (`artifactory.itszuvalex.com`, dead) as `mod`, `api`, `src`, `deobf`, `scaladoc` jars. Femtocraft consumed `ItszuLib:1.7.10-0.1.0-6:deobf`.
- ~9.5k lines: 142 Scala + 5 Java files in `src/main`, 14 test files.

## Inventory and target mapping

Legend: **Port** = same concept, new implementation; **Replace** = the vanilla/Forge mechanism is gone, use the modern equivalent; **Drop** = no longer meaningful. "technolich" means the same author's later framework in `F:\Projects\technolich` (already on 26.1.2), which descends from these classes.

| Area (legacy path under `src/main/scala/com/itszuvalex/itszulib`) | Lines | What it does | 26.1.2 target | Used by Femtocraft |
|---|---|---|---|---|
| `ItszuLib.scala`, `proxy/*`, `Version.scala` | ~150 | `@Mod` object, sided proxies, GUI handler, test block registration | Kotlin `@Mod object` (KFF), `DeferredRegister`, client-only event subscribers instead of proxies. **Done (skeleton)** | — |
| `api/core/Loc4`, `api/OverridableFunction` | 170 | Dimension+xyz location, NBT save, world lookup with test seam | Port (technolich `Loc4`: `ResourceKey<Level>` dimension, codec) | Heavily (41 imports) |
| `api/core/Saveable` + `util/DataUtils.java` | 1080 | Reflection-driven `@Saveable(world, desc, item)` field serialization to NBT | Port onto `ValueOutput`/`ValueInput` + codecs (see DECISIONS B1) | Every tile |
| `api/core/NBTSerializable`, `implicits/NBTHelpers` | 190 | Manual NBT save/load, Scala NBT DSL | Replace with `ValueIOSerializable`-style interface and Kotlin helpers over `ValueOutput`/`CompoundTag` | 17+14 imports |
| `api/core/XMLSerializable`, `configuration/xml` | ~150 | XML config load/save | Drop (XML configs) or replace with `ModConfigSpec`; see `configuration` | No |
| `api/core/Configurable`, `configuration/*` | ~400 | `@Configurable` static fields auto-bound to Forge `Configuration` by classpath scan | Replace with NeoForge `ModConfigSpec`; keep the annotation-driven idea only if Femtocraft needs it | 10 imports |
| `api/access/*`, `api/storage/*` | ~1100 | `IItemAccess` / `IItemCollectionAccess` views over arrays, `IInventory`, NBT; `IItemStorage` (`ArrayItemStorage`); `Revisioned` | Port (technolich `IItemStorage` family) + NeoForge transfer API adapters (`ResourceHandler<ItemResource>`) | `ArrayItemStorage`, via `TileInventory` |
| `api/multiblock/*`, `core/traits/*/MultiBlock*`, `SpatialReactions` | ~350 | Multiblock form/break protocol, controller forwarding | Port (block entity + block state helpers) | Industry frames, cyber machines |
| `api/IPreviewable*`, `render/Previewable*` | ~120 | Ghost preview of multiblocks under the cursor | Replace: client `RenderLevelStageEvent` + 26.1 rendering | Femtocraft previewable renderers |
| `api/player/*`, `player/PlayerProperties` | ~180 | `IExtendedEntityProperties` per-player data + sync | Replace with NeoForge data attachments (`AttachmentType`, `.sync()`) | No direct import |
| `api/events/*`, `api/utility/TileEntityRelocation`, `TileSave` | ~250 | Pick up/place a tile with its data (spatial relocation) | Port on data components (`BLOCK_ENTITY_DATA`) or drop | No direct import |
| `core/TileEntityBase`, `core/TileContainer` | ~160 | BE base: save/load via `@Saveable`, server/client update gating, GUI open, item NBT; `BlockContainer` base opening GUIs and forwarding break | Port as `BlockEntity` + `BaseEntityBlock` bases with `BlockEntityTicker`, `MenuProvider`, `preRemoveSideEffects` | 34 + 29 imports |
| `core/traits/tile/*` | ~550 | Mixins: inventory (`ISidedInventory`), fluid tank(s), description packet, segmented/sided inventories, multiblock component | Port as Kotlin interfaces/helpers + capabilities (`Capabilities.Item/Fluid.BLOCK`, `ResourceHandler`) and `getUpdateTag`/`ClientboundBlockEntityDataPacket` | Yes |
| `core/traits/block/*` | ~450 | Droppable inventory, rotate on place, 6-way "MultiSided" metadata textures | Replace with block states (`FACING`) + `preRemoveSideEffects` drops | Yes |
| `container/*`, `gui/*` | ~1500 | `Container` bases, shift-click logic, a small GUI widget toolkit (panels, flow layout, buttons, text box, fluid tank, item stack) | Replace with `AbstractContainerMenu` + `MenuType` and `AbstractContainerScreen` (+ `GuiGraphics`); widget toolkit ported onto 26.1 widgets where still needed | Yes (GUIs) |
| `network/*` | ~500 | SimpleNetworkWrapper channel + messages (container update, fluid slot click, fluid tank update, player property, GUI item stack) | Replace with `CustomPacketPayload` + `StreamCodec` via `RegisterPayloadHandlersEvent` | `MessageFluidTankUpdate`, `PacketHandler` |
| `logistics/*` | ~650 | `INetwork`/`TileNetwork`/`ManagerNetwork` graph networks of tiles, `LocationTracker` | Port (technolich `TileNetwork`/`NetworkManager`/`LocationTracker`, server tick events) | Yes |
| `pathfinding/*` | ~350 | BFS/DFS/simple pathfinders over `Loc4` | Port (pure logic, unit-testable) | No direct import |
| `render/*` | ~1060 | Immediate-mode `Tessellator` helpers, quad/model math, `Vector3`/`Point3D`, GLSL shader loader | Replace: 26.1 render pipeline (`BlockEntityRenderer` with render states, `VertexConsumer`, `RenderPipeline`); keep `Vector3`-style math or use JOML | `Vector3`, `RenderUtils` |
| `util/*` | ~1600 | Color, comparators (ID/damage/NBT wildcard), inventory utils, player utils, string utils, `ValueTracker`, file utils | Port; comparators become component-aware (`ItemStack.isSameItemSameComponents`), "damage wildcard" becomes item-only match | Yes |
| `PlayerUUIDTracker`, `InterModComms`, `command/*` | ~300 | UUID↔name cache, IMC callback (empty), command base | `GameProfileCache` covers UUIDs; IMC via `InterModProcessEvent`; commands via Brigadier `RegisterCommandsEvent`. Port only if used | No |
| `testing/*` | ~520 | In-game test blocks (portal shader, tank, loc tracker, previewable) | Replace with dev-only content + game tests (`dev/`) | No |
| `assets/itszulib/shader/portal.*`, `textures/gui/GuiInventoryBase.png` | — | Shader for the portal test block; generic inventory GUI texture | Keep texture; shader only if the portal effect is ported | — |
| `src/test/scala/**` | 14 files | ScalaTest suites for `api/access`, `api/storage`, networking | Rewrite as JUnit 5 (Kotlin) against the ported classes | — |

Legacy Scala/Java sources are kept under `src/main/scala` and `src/test/scala` as read-only reference (not compiled; the Scala plugin is not applied) and are deleted as each area is ported.

## Progress

- [x] Branch `neoforge-26.1` from `develop`.
- [x] Build: ModDevGradle 2.0.148, Gradle 9.2.1, Java 25, Kotlin 2.4.0 + Kotlin for Forge 6.3.0. `./gradlew build` and `./gradlew runGameTestServer` pass with a skeleton mod (1 JUnit test, 1 game test).
- [x] B1 decided: ItszuLib becomes the Kotlin version of TechnoLich's framework (DECISIONS B1).
- [x] Framework ported from technolich@f021246: 00594ed (Loc4, trackers, modules, storage, batteries, adapters, sided storage), 76501a4 (fragment core, colorable, ITEM scope, networks, dev block and game tests). 165 JUnit tests, 10 ItszuLib game tests (+1 vanilla) passing.
- [x] Legacy Scala sources removed (still on `develop`). The inventory table above records what each legacy area was and where its replacement lives.
- [ ] ItszuLib-specific additions (multiblock helpers, menu/screen bases) as Femtocraft needs them.

## What happened to each legacy area

| Legacy area | Outcome |
|---|---|
| `Loc4`, `OverridableFunction`, `logistics/*` (networks, `LocationTracker`) | Replaced by the TechnoLich versions (`api/utility`, `core/Networks.kt`), which descend from them |
| `api/access/*`, `api/storage/*`, `core/traits/tile/TileInventory` | Replaced by `IItemStorage` family + `WrapperResourceHandlerIItemStorage` |
| `@Saveable` + `DataUtils`, `NBTSerializable`, `NBTHelpers`, `TileDescriptionPacket` | Replaced by fragment scoped serialization (LEVEL/DESCRIPTION/ITEM) over Value I/O |
| `TileEntityBase`, `TileContainer`, `core/traits/*` | Replaced by `BlockEntityCore` + fragments, `EntityBlockCore`; traits become fragments in Femtocraft |
| `TileFluidTank`, `TileMultiFluidTank` | Not in TechnoLich; Femtocraft's fluids were placeholders (mapped to water), so no fluid fragment yet |
| `api/multiblock/*`, `MultiBlockComponent` | To be added as fragments when Femtocraft's frames/cyber base are ported |
| `container/*`, `gui/*` | To be replaced with `AbstractContainerMenu`/`AbstractContainerScreen` bases when Femtocraft's menus are ported; the 1.7.10 widget toolkit is not ported |
| `network/*` messages | Dropped; Femtocraft registers its own payloads (`PayloadRegistrar`) as needed |
| `player/*`, `PlayerUUIDTracker`, `InterModComms`, `command/*`, `configuration/*`, `pathfinding/*`, `render/*` (shaders, quad math), `testing/*` | Dropped: unused by Femtocraft's ported scope or superseded by vanilla/NeoForge (data attachments, `ModConfigSpec`, Brigadier, render pipeline) |
