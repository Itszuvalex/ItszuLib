# Review findings

Design and correctness findings from reviewing the 26.1 port: what was fixed (with the regression test that pins it)
and what is still open. Decisions are in [DECISIONS.md](DECISIONS.md); the port log is in [PORTING.md](PORTING.md).

## Fixed

Bugs inherited from 1.12.2 (or introduced by porting it). Every fix has a test that fails on the old code. The multiblock rows for `MultiBlockInfo`, `IBlockPattern`, `MultiblockStateHolder` and `MultiblockStatic` describe code
since replaced by the controller-less multiblocks (DECISIONS D11); their tests went with it. The sided configuration
rows still hold (`MultiblockSidedConfigurationTest`), and the `MultiblockUtils` neighbour check is now
`MultiblockFaces.isFacingSameStructure` (`MultiblockSidedConfigurationTest.NeighbourUnloaded_TreatedAsExternalWithoutLookingItUp`).

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
| R18 | `MenuCore` shift-click (was open finding O1) | Vanilla's `moveItemStackTo` and the port's `quickMoveStack` changed `Slot.getItem()` in place and only called `setChanged`. Behind a storage that returns copies from `get` (`ItemStorageNBT`, `ItemStorageResourceHandler`), merging into a slot lost the items and a partial move out of one duplicated them | `MenuCore` writes changed slots back with `Slot.set`; every other click path already wrote back | game test `menu_slots_over_copy_returning_storage` |
| R19 | `IItemStorage.insert` (was open finding O2) | Never asked `canInsert`, and `transferSlotIntoStorageSlot` inserts through it, so any caller could put anything into a filtered slot. `ItemStorageSlice` and `ItemStorageAggregate` did not forward `canInsert` or `maxStackSize`, so a view of a filtered or limited storage accepted anything | `insert` returns the stack when `canInsert` refuses; `insertUnchecked` is the owner's way to fill slots that refuse outside insertion (e.g. outputs). Slices and aggregates forward both | `ItemStorageArrayTest.Insert_CanInsertRefuses_ReturnsStackUnchanged`, `InsertUnchecked_CanInsertRefuses_StillInserts`, `TransferSlotIntoStorageSlot_TargetRefuses_SourceKeepsItems`, `ItemStorageSliceTest.Slice_ForwardsCanInsertAndMaxStackSize`, `ItemStorageAggregateTest.Aggregate_ForwardsCanInsertAndMaxStackSize` |
| R20 | `ItemStorageResourceHandler`, `BatteryEnergyHandler` (was open finding O4) | Opened root transactions, which throw while a transaction is open: behind ItszuLib's own NeoForge wrapper (a storage forwarding to another mod's handler, exposed as a capability), any insert from another mod crashed | `Transactions.openJoined`: nested in the caller's transaction when one is open (the change commits or rolls back with it), root otherwise; while a transaction is closing (a wrapper's journal restoring what it saw) the write is skipped, since the backing handler restores itself | `EnergyAdapterTest.BatteryEnergyHandler_InsideAbortedTransaction_RolledBack`, `BatteryEnergyHandler_InsideCommittedTransaction_Kept`, `BatteryEnergyHandler_BehindWrapper_AbortRestoresWithoutThrowing`, game test `handler_adapter_joins_open_transaction` |

## Open

Not fixed; each needs a maintainer call or is a documented limitation. Numbers are kept when an item closes (O1 is
R18; O2 is R19; O3, multiblock state across chunks, was decided as DECISIONS D11; O4 is R20; O5 was decided as DECISIONS D6).

None open.
