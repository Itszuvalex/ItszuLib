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

**Addendum (maintainer, 2026-10-02): membership events.** Mods need to react when players change team (a machine that
works for its owner's team, a claim that should follow its owner). After any change to `ItszuLib.TEAMS`, each player
whose team differs gets a `TeamMembershipChangedEvent` on the game bus: joining, leaving, removal, each member of a
disbanded team, and a new player's first solo team (`from` null). It is posted after the change, so team data has been
merged (joining) or copied (leaving). The diff is `TeamState.membershipChanges(old, new)`, testable without a game.

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

Recipe viewers draw beside the screen too: JEI's ingredient list covered the side panel, so its faces could not be
clicked. `ComponentScreen.extraAreas()` reports the tab column and the open panel, and an optional JEI plugin
(`compat/jei/ItszuLibJeiPlugin`, compiled against JEI's API only and loaded only when JEI is installed) hands them to
JEI for every `ComponentScreen`, so JEI keeps clear of them.

The dev machine uses all of it (`DevScreen`: tank and energy gauges, the "IO" tab). Not done: textured styles.

**Addendum (maintainer, 2026-10-03): layout helpers.** Screens no longer have to hand-place every coordinate
(`client/screen/Layout.kt`):

- `Row`, `Column` and `Grid` are components holding components: they lay their children out (gap, alignment; a grid in
  equal cells, children centred), pass drawing and input on, and take their size from their children. Hidden children
  take no space. `Spacer` is empty space.
- `addComponent(component, Anchor.X, dx, dy, inContent)` anchors a component in the image or in `contentArea()` (inside
  the border, between the title and the inventory label); margins move inwards from the anchored edges.
- Sizes are worked out each time the screen inits: `ScreenComponent.measure(host)` runs before placement (`Label`
  measures its text unless given a `fixedWidth`; containers measure their children), then `init` once, so children add
  their widgets once.
- The arithmetic is `LayoutMath` (pure, unit tested). Slots stay where menus put them (both sides must agree).

**Addendum (maintainer, 2026-10-02): multiblocks in the side configuration view.** A member of a formed multiblock
showed only its own block, so configuring a structure meant opening each member's screen. Now the menu's side
configuration covers the structure: `MenuSideConfig.members()` is the block entity plus the other loaded members with
the same structure id, `ACTION_SIDE_CONFIG` data carries the member's offset from the menu's block (6 bits per axis,
signed; data without one means the menu's own block, as before), and the server refuses offsets that are not a
member. `SideConfigPanel` draws the whole structure, centred and scaled to fit however it is turned, with every
member's outer faces shaded by its own configuration; faces between members are not drawn (multiblock configurations
lock them). Neighbours outside the structure are not shown in this view. The dev multiblock's menu has it (game test
`multiblock_side_config_reaches_every_member`).

## D13. Tech trees: datapack technologies, team research with progress — DECIDED (maintainer, 2026-10-02)

The maintainer asked for the 1.7.10 Femtocraft tech tree (technologies with prerequisites, a research screen laid out
as a layered graph) in ItszuLib, noting the layout may change. Its 61 technologies stay Femtocraft content; ItszuLib
keeps only the mechanism.

- **Definitions.** Technologies are entries of the synced datapack registry `itszulib:technology`
  (`data/<ns>/itszulib/technology/*.json`, `research/Technology.kt`): tree, prerequisites, cost, icon, optional name,
  description, position, `hidden`, `unlocked_by_default`. A registry (loaded at world load, synced to clients, ids
  shared) rather than a reloadable resource listener, so a `/reload` cannot change what a team can research mid-game.
  Mistakes (unknown prerequisites, cycles) are logged at server start (`Technologies.problems()`), not fatal.
- **Research belongs to teams** (D10), not players: a solo player's team is theirs alone, and joining shares research.
  `Research` team data gained `progress` (partial progress per technology; positive, never for an unlocked id). Its
  codec still reads the old list-only form, so saves from before progress load. Merging keeps the larger progress.
- **Rules** (`research/TechTree.kt`, pure `Technologies`): researched (stored, or `unlocked_by_default`); available
  (every prerequisite researched); locked (shown, not available); hidden (`hidden` and not available). The
  1.7.10 "discovered" state is replaced by available/locked, so locked technologies show what is coming unless hidden.
- **Progress.** What produces it is the mod's choice. `TechTree.addProgress(server, team, id, amount)` takes only what
  the cost still needs (returned, so a machine keeps the rest), unlocks at the cost and posts
  `TechnologyResearchedEvent`; `TechTree.unlock` forces. Every change syncs the team, so mods add progress in batches.
  Gating: `TechTree.isResearched(player, id)` on either side (the client reads its synced team).
- **Layout** (`research/TechTreeLayout.kt`): the 1.7.10 layered layout, reimplemented: columns by longest prerequisite
  path (left to right), waypoints on links that skip columns, barycentre ordering sweeps keeping the fewest crossings
  (the 1.7.10 code hill-climbed with an O(n⁴) crossing count), then rows pulled towards neighbours with a minimum gap.
  A technology's `position` overrides it, which leaves room for a different layout later.
- **Screen.** `client/screen/TechTreeView`, a `ScreenComponent`: icons framed by state, progress bars, links, tooltips
  (name, description, progress, missing prerequisites), drag or scroll to pan, click to select (`onSelect`). Mods put
  it in their own screens; there is no standalone research screen or item.
- **Commands.** `/itszulib research unlock <player> <id>` and `progress <player> <id> <amount>` (operators).
- **Dev data.** Test technologies ship in the jar but load only in dev, through the load condition
  `itszulib:dev_environment` (`util/DevEnvironmentCondition.kt`, registered in production so the files are skipped
  there rather than failing).

Not done: research screens beyond the component.

**Addendum (maintainer, 2026-10-02): costs beyond one number, and rewards.**

- A technology may also need `resources` (named amounts the mod produces, such as computation; `TechTree.addResource`)
  and `items` to hand in (NeoForge `SizedIngredient`s; `TechTree.deliver` takes them from any `IItemStack`s and
  `deliverFrom` from vanilla stacks, a player's inventory or a machine's slots). The rule is pure
  (`Technologies.deliver`): a plain ingredient matches by item id through the `IItemStack` seam, so it is unit tested
  without a game; custom ingredients go through the vanilla stack. It unlocks when its progress, every resource and every item are complete,
  whichever contribution completes it. `Technologies.remaining` says what is left. Progress on these is kept per
  requirement in `Research.requirements` (merged by the larger amount, like progress); the codec reads saves without it.
- `rewards` (`ItemStackTemplate`s: item stacks cannot be decoded while datapack registries load) go to every member of
  the team once: at unlock to those online, otherwise when they log in, and to players joining the team later
  (`TechTree.claimRewards`). Claims are recorded per player in `Research.claimed`; they travel with the player
  (joining unions them, leaving copies them), so moving between teams does not pay out twice. Rewards are items only;
  mods do anything else from `TechnologyResearchedEvent`.
- `TechTreeView` lists resources and items with what is in, and the rewards. Commands:
  `/itszulib research resource <player> <id> <resource> <amount>` and `deliver <player> <id>` (from the player's
  inventory). Resource names translate as `research_resource.<namespace>.<path>`.

**Addendum (maintainer, 2026-10-02): a team research queue.** A mod asked for one shared research focus per team that
every research machine works on, chosen from anywhere, instead of each machine keeping its own choice. That is generic,
so it lives here:

- `Research` gained `queue`: technologies to research next, in order (no duplicates, nothing unlocked). Unlocking takes
  a technology off it; merging puts the team's queue first, then the joiner's additions; the codec reads saves without
  one.
- `Technologies.pathTo(id, research)` is what getting `id` takes (unresearched prerequisites, each after its own, then
  `id`; empty if unreachable). `TechTree.queue(server, team, id)` appends that path, so queueing a far technology
  queues its prerequisites first. `TechTree.unqueue` removes a technology and the queued ones that need it.
- `Technologies.focus(tree, research)` / `TechTree.focus(server, team, tree)` is the first queued technology of a tree
  the team can research now: what a machine should work on. A queue can span trees; each tree has its own focus.
- `TechTreeView` shows queue places as badges and in tooltips, and takes right-clicks (`onAlternate`), so a screen can
  queue on click and unqueue on right-click.
- Commands: `/itszulib research queue|unqueue <player> <id>`.

## D14. Crash-safe server data stores, generalised from teams — DECIDED (maintainer, 2026-10-02)

Teams were persisted by `TeamStore` rather than vanilla `SavedData` (D10), and other server data (saved places,
registries of things in the world) needs the same protection. The mechanism is now generic (`store/`):

- `SafeStore<T>(file, format)`: one immutable value in one file, with a `StoreFormat<T>` (empty value, encode,
  strict decode). Backup fallback, the unreadable file moved aside, refusal to save for the session when neither file
  reads, and temp-file + read-back + atomic-move saves, exactly as teams had.
- `StoreManager<T>`: the value's one place of change: `change { old -> new }` on the server thread (an exception
  changes nothing), change listeners, dirty tracking, `save`.
- `ServerStores.register(id, manager, format)`: loads at server start from `<world>/data/<namespace>/<path>`, saves
  with the overworld, saves and unloads at server stop.

`TeamStore` is now a `SafeStore<TeamState>` (its format repairs what `TeamState.repaired` can), `TeamManager` a
`StoreManager<TeamState>`, registered as `itszulib:teams.dat`; the file and its behaviour are unchanged.

## D15. Producer/consumer distribution over networks — DECIDED (maintainer, 2026-10-02)

Several systems move an amount between producers, storage and consumers over a network each tick (power over
conduits, and later computation over cables). This is energy-style distribution, not the task/worker matching some
mods build. ItszuLib now has one resource-agnostic algorithm for it (`core/Distribution.kt`):

- `Distributable` (max, amount, room, per-tick `transferMax`, add, remove) and `DistributableBattery` for an
  `IBattery`.
- `DistributionAlgorithm(producers, storage, consumers).distribute()`: producers (then storage) give to consumers
  (then storage), within transfer limits; producers with the least room give first, consumers with the least fill
  first; surplus fills storage, shortfall drains it. Each step removes from the source only what the sink accepted.
- `DistributingTileNetwork`: a `TileNetwork` that distributes over its participants at the end of every tick. Nodes
  that are `IDistributionNode`s bring `DistributionParticipant(key, role, resource)`s; a key takes part once, so a
  block (or multiblock) reached through several nodes is not counted twice. `lastResult` keeps what moved.

Not done: per-connection throughput caps (every participant has its own transfer limit; the network has none).

**Addendum (2026-10-02): priority.** `Distributable.priority` (default 0) orders participants within a role before
the usual order: higher gives (or takes) first. Computation uses it to spend the most efficient computers first; it
also suits renewable generators before fuel burners.

---

## D16. Screen themes, grain and slot looks — DECIDED (maintainer, 2026-10-02)

The maintainer asked for screens that are not only vanilla grey: a dark mode like the old Femtocraft GUIs, as a theme
system that mods and others can extend; the old GUIs' faint per-pixel grain, as a toggle; and slots that always show
where items go, since generated screens draw no art.

- **Themes** (`client/screen/ScreenTheme.kt`): a `ScreenTheme` is a palette (panel and its bevel and outline, slot
  face and inset edges, output ring, gauge frame and well, text and muted text, progress, energy) plus grain
  strength. Built in: `itszulib:light` (vanilla's grey with its bevel and inset slots) and `itszulib:dark` (sampled
  from the old Femtocraft art: panel `#1E1E1E`, `#3E4040` bevel, slots `#292929` inset `#0F0F0F`/`#3E4040`).
- **Defining themes.** Mods and resource packs add JSON files at `assets/<namespace>/itszulib/themes/<path>.json`
  (id `<namespace>:<path>`, reloaded with resources): `parent` (default `itszulib:light`; may be another file),
  `colors` (any of `panel`, `panel_light`, `panel_dark`, `outline`, `slot`, `slot_shadow`, `slot_light`,
  `slot_output`, `frame`, `well`, `text`, `text_muted`, `progress`, `energy`, `button`, `button_hover`, as `#RRGGBB` or `#AARRGGBB`; unknown
  names are an error) and `grain`. A file may replace a theme of the same id, built-ins included. Code can
  `ScreenThemes.register`. Parent cycles and unknown parents are logged and the theme skipped.
- **Choosing.** Each `ComponentScreen` kind has a default (`defaultTheme()`, `itszulib:light` unless overridden). The
  client config (`config/itszulib-client.toml`, editable from the Mods screen) can force one theme for every screen
  (`theme`) and turn grain off (`grain`); both apply live. Components draw through `ScreenStyle`, which the screen
  points at its theme before drawing, so existing components follow it; `Label`, `ProgressBar` and `EnergyGauge` use
  the theme's colours unless given one.
- **Grain** (`ScreenGrain`): per-pixel value noise like the old GUIs' (most pixels a shade lighter or darker, about
  one in 160 a stronger speck), as a 128x128 tile drawn white or black with alpha, tiled over panels at the theme's
  strength, so it changes value but not hue.
- **Slots.** Every slot is drawn as an inset. Take-only slots (`OutputSlot`, or any slot whose `SlotLook.isOutput`)
  get a ring in the theme's output colour, drawn under all insets so neighbouring outputs share one outline. A slot
  may give a hint (`SlotLook.hint()`; `addStorageSlots(..., hint = stack)`) shown faded while it is empty, to say
  what goes there. Screens with hand-drawn art can still call `extractSlots` for these.
- Title and inventory labels use the theme's text colour.
- **Side configuration background** (maintainer): the 3D side configuration view has its own background toggle (the
  "Bg" button beside the mode button), dark (the theme's well) or light, whichever shows the blocks better; it is
  remembered in the client config (`sideConfigLight`).

- **Buttons** (maintainer, 2026-10-02): side panel tabs and the side configuration buttons are `ThemedButton`s, drawn
  by `ScreenStyle.button` in the theme: raised in the theme's `button` colour, `button_hover` while hovered, pressed in
  (a slot's inset) while selected (the open panel's tab, the light background toggle), flat with muted text while
  inactive. Both colours are optional in code and JSON (they default to the panel and its light bevel). Mods use
  `ThemedButton` for their own buttons; a `SidePanel` may give an item `icon` for its tab instead of a label.
- **Accents** (maintainer, 2026-10-02): a `ThemedButton` (and a `SidePanel` tab) may name an accent that tints its
  face, so the same kind of button looks the same on every screen: `ButtonAccents.IO` (blue; the side configuration
  tab has it), `UPGRADE` (green), `DANGER` (red), `INFO` (amber). Themes recolour or add accents (`accents` in a theme
  file, a map of name to colour, merged over the parent's); mods add their own names with defaults through
  `ButtonAccents.register`. The tint is stronger while hovered, faint while inactive, and laid over the inset while
  selected.

Not done: themes for screens other than `ComponentScreen`s.

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
