# Porting ItszuLib to NeoForge 26.1.2

Status log and inventory for the port on branch `neoforge-26.1`. Decisions live in [DECISIONS.md](DECISIONS.md).

## Source branch

The port first started from GitHub's `develop` (2016-03-20, Minecraft 1.7.10). GitHub turned out to be a stale
mirror: the newer history (Forge 1.8.9 through 1.12.2, up to Nov 2020) is on GitLab. The port's source is now
**`gitlab/develop-1.12.2-types`** (2020-11-03, "Maybe fix GuiFluidTank to work with Fluids without Blocks"), the
branch Femtocraft's newest work (`develop-1.12.2-v3`) builds against.

`neoforge-26.1` records it as an ancestor with `git merge -s ours` (e08a911): the tree is unchanged, because the 26.1
framework is the Kotlin port of TechnoLich's framework (DECISIONS B1), which itself descends from this 1.12.2 code.
What the 1.12.2 branch adds over the framework is ported on top, listed in [1.12.2 features](#1122-features-to-port).
See DECISIONS D2.

| Branch (GitLab) | Last commit | Used? |
|---|---|---|
| `develop-1.12.2-types` | 2020-11-03 | **Yes** (merged `-s ours`; features ported below) |
| `develop-1.12.2`, `develop`, `master` | 2020-04-26 / 2020-03-28 | No: behind `develop-1.12.2-types` |
| `develop-1.12.2-indirectless_network`, `develop-1.12.2-sidedfix` | 2020-04-01 / 2017-11-09 | No: side branches |
| `develop-1.11.2`, `develop-1.11`, `develop-1.10`, `develop-1.8.9` | 2016-2017 | No: older Minecraft versions |
| `develop-customrender`, `develop-network`, `develop-refactoring` | 2015-2016 | No: 1.7.10 side branches (see the history below) |

## 1.12.2 features to port

`develop-1.12.2-types` is Scala on Forge 1.12.2: ~266 files, ~9.7k lines. Its "modules" (`TileEntityModule`,
`TileEntityInternalModule`) are what the framework calls fragments, and its `ITileEntity`/`IWorld` wrappers are the
framework's `IBlockEntity`/`ILevel` adapters. Most of it is already covered by the framework. This list is what the
framework lacks, in the order it is ported. "Femtocraft v3" is the usage in Femtocraft `develop-1.12.2-v3`.

| # | Feature | 1.12.2 source | Framework target | Femtocraft v3 needs it for | Status |
|---|---|---|---|---|---|
| F1 | Fluid stacks and storage | `api/wrappers/IFluidStack`, `WrapperVanillaFluidStack`, `api/storage/IFluidStorage`, `IFluidStorageModifiable`, `FluidStorageArray`, `FluidStorageModifiableSlice`, `Dynamic*` | `api/adapters/IFluidStack`, `api/storage/IFluidStorage` family (fill/drain algorithms kept), `WrapperResourceHandlerIFluidStorage` (NeoForge fluid `ResourceHandler`, transactional) | Liquifier, germination chamber, fluid repository | Planned |
| F2 | Sided fluid configuration and automatic IO | `core/SidedFluidStorageConfiguration`, `ModuleIItemAutoIO`, `ModuleIFluidAutoIO`, `util/TileEntityUtils.checkDo*IO`, `Module*SidedConfiguration` | `SidedFluidStorageConfiguration`; `FragSidedConfiguration`, `FragItemAutoIO`, `FragFluidAutoIO` (push/pull through neighbours' `ResourceHandler` capabilities) | Every machine's side config GUI and auto IO | Planned |
| F3 | Storage fragments and capability exposure | `ModuleIItemStorage`, `ModuleIFluidStorage`, `ModuleI*HandlerConverter` | `FragItemStorage`, `FragFluidStorage`: expose `Capabilities.Item/Fluid.BLOCK` per side (sided config aware) and save the storage | Every machine | Planned |
| F4 | Multiblocks | `api/multiblock/*` (`IBlockPattern`, `BlockPatternStatic`, `IMultiblock`, `MultiblockStatic`, `MultiBlockInfo`, `MultiblockStateHolder`, `MultiblockUtils`, `Multiblock*StorageConfiguration`), `ModuleMultiblockInfo`, `TileEntityMultiblockTickableModule`, `ModuleMultiblock*` | `api/multiblock`: patterns with rotation, form/break protocol, `MultiBlockInfo` exposed through a `MULTIBLOCK` module by `FragMultiBlockInfo`, controller-held state, controller-forwarding storage and menu fragments | Frame, germination chamber, focusing chamber, reformer | Planned |
| F5 | Menus and value sync | `container/ContainerBase`, `ContainerInv`, `container/sync/*` (`ISync`, `SyncInt/Long/Float/Double/StringArray/EnumAutomaticIOArray/IItemStack/IFluidStack`), `MessageSync` | `menu/MenuCore` (`AbstractContainerMenu`: storage slots, shift-click, typed syncs sent by a `MenuSyncPayload` with `StreamCodec`s) and `MenuActionPayload` (client to server: action id + data, for side config and similar buttons) | Every GUI | Planned |
| F6 | Open a menu from a block | `ModuleGui`, `ModuleMultiblockGui`, `gui/ItszuGuiHandler` | `FragMenu` (opens a `MenuProvider` on use, writes the position for the client), multiblock variant forwards to the controller | Every GUI block | Planned |
| F7 | Network messages | `network/PacketHandler`, `MessageBase`, `MessageUpdateNBT`, `MessageSync` | `network/PacketHandler` (as in TechnoLich) + the payloads in F5. `MessageUpdateNBT`'s job is done by the description packet; `MessageContainerUpdate` by `DataSlot`s | Femtocraft payloads | Planned |
| F8 | Horizontal facing | `core/behaviors/BlockBehaviorHorizontalFacing`, `BlockBehaviors` | `HorizontalEntityBlockCore` (`FACING` state, placed facing the player, rotate/mirror) | Almost every machine | Planned |
| F9 | Sided connections and wire networks | `ModuleSidedConnectable`, `ModuleSidedBlockableConnectable`, `ModuleNetworkedWire`, `logistics/IPersistedConnectableNetworkNode`, `util/FaceBitSet` | `util/FaceBitSet`, `core/frag/FragConnectable` (connected/blocked faces), `IPersistedConnectableNetworkNode`, `FragNetworkedWire` on `TileNetwork` | Power conduit, computation conduit, logistics conduit | Planned |
| F10 | Power-driven task | `util/Task` | `util/Task` (progress, speed/efficiency scaling) | Machines' processing | Planned |
| F11 | Screen helpers | `gui/GuiBase`, `GuiFluidTank`, `GuiProgress`, `GuiPanelTexture`, `GuiLabel` | `client/ScreenHelpers`: draw a fluid tank (fluid sprite + tint), progress bars, tooltips. The 1.12.2 widget toolkit (flow layouts, text box, panels) is not ported | Every screen | Planned |

Not ported from 1.12.2, and why:

- `container/sync` item/fluid slot syncs and `MessageIItemStackSyncClick`: 1.12.2 replaced vanilla slots with its own
  synced slots. 26.1 menus keep vanilla `Slot`s over `WrapperContainerIItemStorage`, which already sync and handle
  clicks (DECISIONS D6).
- `MessageFluidSlotClick`, `MessageFluidTankUpdate`: commented out on the 1.12.2 branch.
- `api/client/IPreviewable*`, `render/*` (OBJ renderer, `RenderUtils`, `TileEntityRenderCube`, shaders): dynamic
  rendering is follow-up work (Femtocraft DECISIONS B2).
- `initialization/*` (`BlockBuilder`, `InitializationManager`): `DeferredRegister` does this.
- `implicits/*`, `api/Overridable`, `api/Burnable`, `ManagerCapabilities`/`DummyStorage`: Scala/Forge-capability
  plumbing with Kotlin or NeoForge equivalents.
- `configuration/*`, `command/*`, `pathfinding/*`, `xml/*`, `PlayerUUIDTracker`, `InterModComms`: unused by
  Femtocraft v3, or replaced by `ModConfigSpec`/Brigadier (same as the 1.7.10 outcome below).
- `api/utility/TileEntityRelocation`, `TileSave`: unused by Femtocraft v3.
- `util/*` helpers that Femtocraft v3 uses (`PlayerUtils`, `ChatHelper`, `Comparators`): ported into Femtocraft or
  replaced by vanilla calls where they are one-liners.

## History: the 1.7.10 port

The sections below describe the first pass, from GitHub `develop` (1.7.10). They are kept as the record of where
each legacy area went.

| Branch (GitHub) | Last commit | Relation to `develop` | Used? |
|---|---|---|---|
| `develop` | 2016-03-20 | (base) | First pass |
| `develop-refactoring` | 2016-03-20 | identical to `develop` (0 ahead / 0 behind) | same commit |
| `develop-customrender` | 2016-03-02 | 3 ahead, 26 behind: a WIP VBO/OBJ loader ("no luck yet really") | No: modern MC has its own model pipeline (NeoForge OBJ loader) |
| `develop-network` | 2015-09-25 | 1 ahead ("TileNetwork cleanup"), 51 behind | No: superseded by later `develop` network code |
| `master` | 2021-04-26 | 2 ahead (README edits only), 30 behind | No |

### What the 1.7.10 original targets

- Minecraft **1.7.10**, Forge `10.13.4.1448`, ForgeGradle 1.2, Java 7.
- Scala 2.11.8 (`modLanguage = "scala"`), ScalaTest/ScalaMock tests; a few Java files.
- Published to a private Artifactory (`artifactory.itszuvalex.com`, dead) as `mod`, `api`, `src`, `deobf`, `scaladoc` jars. Femtocraft consumed `ItszuLib:1.7.10-0.1.0-6:deobf`.
- ~9.5k lines: 142 Scala + 5 Java files in `src/main`, 14 test files.

### 1.7.10 inventory and target mapping

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
- [x] Source moved to GitLab `develop-1.12.2-types`, recorded with `merge -s ours` (e08a911).
- [ ] 1.12.2 features F1-F11 (table above).

### What happened to each 1.7.10 area

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
