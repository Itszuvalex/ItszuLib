package com.itszuvalex.itszulib.api;

import com.itszuvalex.itszulib.api.multiblock.MultiBlockInfo;
import com.itszuvalex.itszulib.api.storage.IFluidStorage;
import com.itszuvalex.itszulib.api.storage.IItemStorage;
import com.itszuvalex.itszulib.util.Color;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

/**
 * Created by Chris on 1/2/2017.
 */
public class ItszuLibCapabilities {
    @CapabilityInject(IItemStorage.class)
    public static Capability<IItemStorage> ITEM_STORAGE = null;

    @CapabilityInject(IPreviewable.class)
    public static Capability<IPreviewable> ITEM_PREVIEWABLE = null;

    @CapabilityInject(IFluidStorage.class)
    public static Capability<IFluidStorage> FLUID_STORAGE = null;

    @CapabilityInject(MultiBlockInfo.class)
    public static Capability<MultiBlockInfo> TILE_MULTIBLOCK = null;

    @CapabilityInject(Color.class)
    public static Capability<Color> COLORABLE = null;

}
