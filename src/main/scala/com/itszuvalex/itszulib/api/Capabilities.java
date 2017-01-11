package com.itszuvalex.itszulib.api;

import com.itszuvalex.itszulib.api.storage.IItemStorage;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;

/**
 * Created by Chris on 1/2/2017.
 */
public class Capabilities {
    @CapabilityInject(IItemStorage.class)
    public static Capability<IItemStorage> ITEM_STORAGE = null;

    @CapabilityInject(IBurnable.class)
    public static Capability<IBurnable> ITEM_BURNABLE = null;
}
