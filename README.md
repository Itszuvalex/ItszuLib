# ItszuLib

A shared framework for NeoForge mods, written in Kotlin. It provides the pieces that tech mods keep rebuilding:
block entities assembled from reusable parts, storage with transfer-API adapters, networks of blocks, multiblocks,
menus and screens, teams, crash-safe server data and tech trees. It knows nothing about the mods built on it.

- **Minecraft 26.1.2 / NeoForge 26.1.2.112 / Java 25**, Kotlin 2.4.0 on Kotlin for Forge 6.3.0 (a required runtime
  mod), built with ModDevGradle.
- Mod id `itszulib`, package `com.itszuvalex.itszulib`, GPL-2.0-or-later.
- Branches: `main` is the 26.1 line. `develop`, `develop-1.12.2*` and the older `develop-*` branches hold the Minecraft
  1.12.2 (Forge, Scala) code and earlier versions. The full history of the former GitLab repository
  (https://gitlab.com/Itszuvalex-Minecraft/ItszuLib) is here.

`AGENTS.md` covers building, testing and the API in detail; `docs/DECISIONS.md` records why things are the way they
are.

## What it adds

### Block entities built from fragments

`BlockEntityCore` is a block entity whose behaviour is a list of **fragments**: small, reusable parts (an inventory,
a tank, a battery, a colour, a menu, multiblock membership) added in the block entity's `init`. Fragments get the
vanilla lifecycle (load, save, client sync, removal, chunk unload, ticking) without each block entity wiring it by
hand, so one machine is a few lines of composition instead of a class hierarchy.

*For:* sharing machine behaviour across many blocks without inheritance chains.

### Modules and capabilities

A **module** (`IModule<T>`) is a named handle to something a block entity offers (a storage, a configuration, a
network node), optionally backed by a NeoForge block or item capability. Code inside the mod asks block entities for
modules directly; other mods see the capability. One registration call per block entity type exposes all of them.

*For:* one way to ask "what does this block offer on this side", inside and outside the mod.

### Scoped serialization

Fragments save into three scopes: **LEVEL** (the world save), **DESCRIPTION** (what clients see) and **ITEM** (what
stays with the block when it is broken and placed again). Each fragment writes into its own keyed child with Value
I/O and codecs.

*For:* saving, syncing and item-carried data from one description, with no hand-written NBT plumbing.

### Item, fluid and energy storage

`IItemStorage`, `IFluidStorage` and `IBattery`, with array, slice, aggregate and dynamic implementations, change
notifications, and insertion rules (outputs refuse outside insertion; owners fill them directly). Adapters connect
them to NeoForge's transfer API in both directions, joining open transactions.

- **Sided configuration** chooses which storage each face exposes, and **automatic IO** pushes and pulls through
  configured faces every tick, for items, fluids and energy.

- **Indexed storage** knows where everything is: each write updates an index of item (or fluid) ids to slots and of
  empty slots, so finding, counting, inserting and extracting touch only the slots that matter. An **index across
  storages** keeps which storages hold each item, so a network of vaults answers "where is iron?" without scanning
  any of them.
- **Filters**: allow- and denylists of items, fluids or anything a mod defines, matching data components or not,
  edited AE2-style by clicking a filter cell with the thing held; an allowlist lets an index fetch exactly what it
  names.
- **Search**: queries of space-separated terms by name, mod (`@`), tooltip (`#`), tag (`$`) or id (`*`), negated with
  `-`; terms that depend only on the item are checked before a single slot is read.

*For:* machine inventories that behave correctly with hoppers, pipes and other mods' transfer code, and large
storage that stays fast to search.

### Networks of blocks and distribution

`TileNetwork` groups connected blocks (wires, conduits, cables) into server-side networks that merge, split and
follow chunk loading. A `DistributingTileNetwork` also runs a **producer/consumer distribution** every tick:
producers and storage give to consumers and storage within each participant's transfer limit, with nothing created
or lost. The algorithm is resource-agnostic, for power, computation or anything else measured as an amount.

*For:* conduit networks without each mod re-solving merging, splitting and fair distribution.

### Multiblocks

Controller-less multiblocks: a **shape** maps offsets to roles; members save their own membership; structures form
when their members load or on request, and break by policy (free the others, or destroy them). A structure's shared
state lives on its home block and is reachable from every member, and the home chunk stays loaded while a member
elsewhere is active.

*For:* large machines that survive chunk boundaries and unloading.

### Menus and screens

`MenuCore` builds container menus from vanilla slots over the storages, with shift-click, typed value syncing and
actions that reach only the player's open menu. `ComponentScreen` builds screens from components (energy and fluid
gauges, progress bars, labels, your own) and side panels behind tabs. Menus with sided configuration get a **3D side
configuration panel**: the machine and its neighbours, rotated by dragging, faces clicked to configure.

Screens are **themed**: a light (vanilla) and a dark theme are built in, mods and resource packs add their own as
JSON palettes, each screen picks a default and players can force one or turn off the panels' faint grain. Generated
screens still show where items go: slots are drawn inset, outputs ringed, and empty slots can show a faded hint of
what belongs in them.

A **storage terminal** gives a menu a view of everything in a storage index: a search box with search modes, sorting
by count, name or id, a paged grid of item kinds with their totals, and click to take or put, shift-click to store.

Recipe viewers are kept clear: with JEI installed, its overlays avoid the side panels and their tabs.

*For:* functional machine screens with little code, and in-world-feeling configuration.

### Teams

Every player is always in exactly one team (solo by default), with invites, roles (owner, officers, members) and
per-team data that mods register. Joining merges data into the team; leaving takes a copy. Team state is immutable
and checks its own invariants, so an operation either yields a valid state or changes nothing.

*For:* shared progress between players, such as research.

### Crash-safe server data

`SafeStore`, `StoreManager` and `ServerStores` persist server data that must not be lost, in place of vanilla
`SavedData` (which replaces unreadable data with an empty instance and later saves over it). Strict decoding, a
backup, moving an unreadable file aside, refusing to save over data that could not be read, and verified atomic
writes. Teams are stored this way.

*For:* saved places, registries of things in the world, anything players build up.

### Tech trees

Technologies are datapack entries (tree, prerequisites, cost, icon, optional position) synced to clients; besides
their cost they may need other resources and items to hand in, and give item rewards to each team member. Research
belongs to teams, with partial progress and a research queue whose first available entry is the team's focus; mods
decide what produces progress and gate content with one call on either side. A layered layout places each tree automatically, and `TechTreeView` draws it in any screen with tooltips,
panning and selection. The same view (`NodeTreeView`) draws any other tree a mod has, such as skills or talents.

*For:* research and progression that pack makers can edit.

### Testing seams

Engine-facing interfaces (`ILevel`, `IBlockEntity`, `IItemStack`) and test fakes let logic be unit tested without a
running game; everything that touches the game is covered by NeoForge game tests (`./gradlew runGameTestServer`).

## Using it

Mods depend on `com.itszuvalex.itszulib:itszulib`, often as a Gradle composite build of a sibling checkout. Build with
`./gradlew build`; see `AGENTS.md` for the full set of tasks and machine notes.
