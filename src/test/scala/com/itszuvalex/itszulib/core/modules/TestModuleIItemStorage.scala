package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.ItszuLibModules
import com.itszuvalex.itszulib.api.storage.{IItemStorage, ItemStorageArray, ItemStorageSlice}
import com.itszuvalex.itszulib.api.wrappers.{IItemStack, ITileEntity}
import com.itszuvalex.itszulib.core.SidedItemStorageConfiguration
import com.itszuvalex.itszulib.{TestBase, TestableItemStack}
import net.minecraft.util.EnumFacing

class TestModuleIItemStorage extends TestBase {

  trait WithModule {
    val storageArray      = new Array[IItemStack](2)
    val item0: IItemStack = new TestableItemStack(1, 1)
    val item1: IItemStack = new TestableItemStack(1, 5)
    storageArray(0) = item0
    storageArray(1) = item1
    val storage                      = new ItemStorageArray(2)
    val module                       = new ModuleIItemStorage(storage)
    val fakeITileEntity: ITileEntity = mock[ITileEntity]
  }

  trait WithSidedConfig extends WithModule {
    val storageSlice0  : IItemStorage         = new ItemStorageSlice(storage, Array(0))
    val storageSlice1  : IItemStorage         = new ItemStorageSlice(storage, Array(1))
    val evenStorageName: String               = "Even"
    val oddStorageName : String               = "Odd"
    val defaults       : EnumFacing => String = facing => if (facing.getIndex % 2 == 0) evenStorageName else oddStorageName
    val config                                = new SidedItemStorageConfiguration(
      defaults
      , Map[String, IItemStorage](evenStorageName -> storageSlice0, oddStorageName -> storageSlice1)
      , () => EnumFacing.NORTH)
  }

  "ModuleIItemStorage" should {
    "have ITEM_STORAGE as its module" in new WithModule {
      module.module should be theSameInstanceAs ItszuLibModules.ITEM_STORAGE
    }
    "with no configurable item storage" should {
      "should return the default storage on null face" in new WithModule {
        module.faceToModuleMapper(fakeITileEntity)(null).get should be theSameInstanceAs storage
      }
      "return the default storage on all faces" in new WithModule {
        (fakeITileEntity.moduleOption[SidedItemStorageConfiguration] _).expects(ItszuLibModules.ITEM_STORAGE_CONFIGURABLE, null)
                                                                       .anyNumberOfTimes().returns(None)
        val mapper = module.faceToModuleMapper(fakeITileEntity)
        EnumFacing.VALUES.
                  foreach(f =>
                            mapper.apply(f).get should be theSameInstanceAs storage
                          )
      }
    }

    "with a configurable item storage" should {
      "should return the default storage on null face" in new WithSidedConfig {
        module.faceToModuleMapper(fakeITileEntity)(null).get should be theSameInstanceAs storage
      }

      "return the correct storage on all faces" in new WithSidedConfig {
        (fakeITileEntity.moduleOption[SidedItemStorageConfiguration] _).expects(ItszuLibModules.ITEM_STORAGE_CONFIGURABLE, null)
                                                                       .anyNumberOfTimes().returns(Some(config))
        val mapper = module.faceToModuleMapper(fakeITileEntity)
        EnumFacing.VALUES.
                  foreach { f =>
                    val expected = if (f.getIndex % 2 == 0) storageSlice0 else storageSlice1
                    mapper.apply(f).get should be theSameInstanceAs expected
                  }
      }

    }

    "haveWorldNBT" in new WithModule {
      module.hasWorldNBT shouldBe true
    }
  }

}
