package com.itszuvalex.itszulib.core.modules

import com.itszuvalex.itszulib.api.ItszuLibModules
import com.itszuvalex.itszulib.api.storage.{FluidStorageArray, FluidStorageModifiableSlice, IFluidStorage}
import com.itszuvalex.itszulib.api.wrappers.{IFluidStack, ITileEntity}
import com.itszuvalex.itszulib.core.SidedFluidStorageConfiguration
import com.itszuvalex.itszulib.{TestBase, TestableFluidStack}
import net.minecraft.util.EnumFacing

class TestModuleIFluidStorage extends TestBase {

  trait WithModule {
    val storageArray        = new Array[IFluidStack](2)
    val fluid0: IFluidStack = new TestableFluidStack(1, 1)
    val fluid1: IFluidStack = new TestableFluidStack(1, 5)
    storageArray(0) = fluid0
    storageArray(1) = fluid1
    val storage                      = new FluidStorageArray(storageArray)
    val module                       = new ModuleIFluidStorage(storage)
    val fakeITileEntity: ITileEntity = mock[ITileEntity]
  }

  trait WithSidedConfig extends WithModule {
    val storageSlice0  : IFluidStorage        = new FluidStorageModifiableSlice(storage, Array(0))
    val storageSlice1  : IFluidStorage        = new FluidStorageModifiableSlice(storage, Array(1))
    val evenStorageName: String               = "Even"
    val oddStorageName : String               = "Odd"
    val defaults       : EnumFacing => String = facing => if (facing.getIndex % 2 == 0) evenStorageName else oddStorageName
    val config                                = new SidedFluidStorageConfiguration(
      defaults
      , Map[String, IFluidStorage](evenStorageName -> storageSlice0, oddStorageName -> storageSlice1)
      , () => EnumFacing.NORTH)
  }

  "ModuleIFluidStorage" should {
    "have FLUID_STORAGE as its module" in new WithModule {
      module.module should be theSameInstanceAs ItszuLibModules.FLUID_STORAGE
    }
    "with no configurable fluid storage" should {
      "should return the default storage on null face" in new WithModule {
        module.faceToModuleMapper(fakeITileEntity)(null).get should be theSameInstanceAs storage
      }
      "return the default storage on all faces" in new WithModule {
        (fakeITileEntity.moduleOption[SidedFluidStorageConfiguration] _).expects(ItszuLibModules.FLUID_STORAGE_CONFIGURABLE, null)
                                                                        .anyNumberOfTimes().returns(None)
        val mapper = module.faceToModuleMapper(fakeITileEntity)
        EnumFacing.VALUES.
                  foreach(f =>
                            mapper.apply(f).get should be theSameInstanceAs storage
                          )
      }
    }

    "with a configurable fluid storage" should {
      "should return the default storage on null face" in new WithSidedConfig {
        module.faceToModuleMapper(fakeITileEntity)(null).get should be theSameInstanceAs storage
      }

      "return the correct storage on all faces" in new WithSidedConfig {
        (fakeITileEntity.moduleOption[SidedFluidStorageConfiguration] _).expects(ItszuLibModules.FLUID_STORAGE_CONFIGURABLE, null)
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
