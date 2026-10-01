# Review findings

Design and correctness findings from reviewing the 26.1 port: what was fixed (with the regression test that pins it)
and what is still open. Decisions are in [DECISIONS.md](DECISIONS.md); the port log is in [PORTING.md](PORTING.md).

## Fixed

Bugs inherited from 1.12.2 (or introduced by porting it). Every fix has a test that fails on the old code.

| # | Area | Problem | Fix | Regression test |
|---|---|---|---|---|
| R1 | `MultiBlockInfo.form` | Compared the block's own position with the stored controller, so another multiblock could take over a formed controller | Compares controllers | `MultiBlockInfoTest.Form_AlreadyFormed_OnlySameControllerAccepted` |
| R2 | `IBlockPattern.rotated` | Rotated a throwaway copy | Immutable pattern, returns the rotated copy | `BlockPatternStaticTest.Rotated_RotatesOffsetsAndRecordsRotation` |
| R3 | `Multiblock*StorageConfiguration` | Relative faces passed to a world-face check; relative queries ignored internal faces | Converts first; relative queries overridden | `MultiblockSidedConfigurationTest.*` |
| R4 | `MultiblockStateHolder` | A broken controller kept its state until the chunk reloaded | Dropped when no longer a formed controller | `FragMultiblockStateTest.BreakThenReform_StartsFromFreshState` |
| R5 | Multiblock and auto IO neighbour lookups | Would load neighbouring chunks | `isLoaded` first | `MultiblockUtilsTest.IsFacingInMultiblock_NeighbourUnloaded_*`, `FragAutoIOTest.Tick_NeighbourUnloaded_NotLookedUp` |
| R6 | `MessageSync` | Applied to any open container of the same GUI id | Payload carries the container id | `MenuCoreSyncTest.Apply_OtherContainerId_Ignored` |
| R7 | `ContainerInv.transferStackInSlot` | Gave up instead of moving between inventory and hotbar | Falls back | game test `menu_quick_move` |
| R8 | `ModuleNetworkedWire` | One-sided disconnects, stale flags after offline removal, force-loaded neighbours | Both sides cleared; faces reconciled on load; unloaded neighbours skipped | `FragNetworkedWireTest.*`, game test `wire_network_forms_and_splits` |
| R9 | `Task` | 0/0 progress was NaN; negative power undid progress | At least one tick; negative power ignored | `TaskTest.*` |
| R10 | `SidedStorageConfiguration` | Unknown key cycled to the wrong end; missing saved keys became `""`; map-order dependent | Fixed ends, defaults on bad data, sorted keys | `SidedStorageConfigurationTest.*` |
| R11 | `IItemStorage.transferSlotIntoStorageSlot` | Wrote back a pre-insert copy of the source: moving a slot into itself, or into an aliasing view, destroyed the items | Re-reads the source after the insert | `ItemStorage*Test.TransferSlotIntoStorageSlot_SameSlot_KeepsItems`, `TransferSlotIntoStorage_IntoItself_KeepsItems`, `TransferSlotIntoStorageSlot_AliasingView_KeepsItems` |
| R12 | `IItemStorage.split` | A negative amount grew the slot | Non-positive amounts return empty | `ItemStorage*Test.Split_NegativeAmount_ChangesNothing` |
| R13 | `IFluidStorage.drain(maxDrain)` | First tank chosen by `canDrain` only, bypassing `canDrainFluidType` | Uses `canDrainFluidType` | `FluidStorageArrayTest.DrainAmount_RespectsCanDrainFluidType` |
| R14 | `MultiblockStatic.form` | Partial formation when a part refused | All or nothing (`MultiBlockInfo.canForm`) | `MultiblockStaticTest.Form_*_ReturnsFalseAndNoPartJoins` |
| R15 | `PowerBattery` | The port dropped 1.12.2's clamp to capacity | Charge kept within `[0, max]` | `EnergyAdapterTest.PowerBattery_*` |
| R16 | `StorageUtils.removeItemsFromStorage` | Never compared items, so any items satisfied a request | Compares items | `StorageUtilsTest.RemoveItemsFromStorage_OtherItems_NotFoundAndNothingRemoved` |
| R17 | `StorageSlot` | Never overrode `Slot.mayPlace` (always true): menu slots ignored `IItemStorage.canInsert` | `mayPlace` asks `canInsert` | game test `menu_storage_slots_honour_can_insert` |

## Open

Not fixed; each needs a maintainer call or is a documented limitation.

| # | Area | Finding | Current handling |
|---|---|---|---|
| O1 | Menus over copy-returning storages | Vanilla menu code (`moveItemStackTo`, `quickMoveStack`) changes `Slot.getItem()` in place and then calls `setChanged`. `ItemStorageNBT` and `ItemStorageResourceHandler` return a fresh copy from `get`, so those edits are lost or duplicate items | Use live storages (`ItemStorageArray`, slices and aggregates of it) behind menu slots. Documented on `WrapperContainerIItemStorage` |
| O2 | `IItemStorage.insert` ignores `canInsert` | As in 1.12.2. Machines rely on it to fill their own output slots; automation goes through `WrapperResourceHandlerIItemStorage` and `transferSlotIntoStorage`, which do check | Kept; callers that act for a player or another block must check `canInsert` |
| O3 | Shared multiblock state across chunks | `FragMultiblockState.get` on a part returns null while the controller's chunk is unloaded | Callers can reject such multiblocks with `IBlockPattern.overChunkBoundaries` |
| O4 | `ItemStorageResourceHandler`, `BatteryEnergyHandler` | Open root transactions; calling them while a transaction is open throws | Documented on both classes |
| O5 | D6 (vanilla menu slots) | 1.12.2's synced-slot click handling is not ported | Awaiting maintainer sign-off ([DECISIONS.md](DECISIONS.md) D6) |

## Framework changes to mirror into TechnoLich

The fragment/module framework is shared with TechnoLich (DECISIONS B1). These changes to shared code were made here:

- `IItemStorage.split`: non-positive amounts return `IItemStack.Empty` (R12).
- `IItemStorage.transferSlotIntoStorageSlot`: re-reads the source slot after the insert (R11).
- `PowerBattery.setStorage`/`setStorageQuietly`: clamp to `[0, maxStorage]` (R15).
- `IBlockEntityBlockEventHandler.onLoad`/`onNeighborChanged` and `EntityBlockCore.useWithoutItem` (DECISIONS D8).

ItszuLib-only code (fluid storage, multiblocks, menus, connectables, `StorageUtils`) has no TechnoLich counterpart.

TechnoLich's Kotlin code (88b2ca5) was compared with this branch on 2026-10-01: it is a copy of ItszuLib's framework
from before these fixes, with no fixes of its own to bring back.
