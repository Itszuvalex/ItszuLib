package com.itszuvalex.itszulib.api.utility

import net.neoforged.fml.LogicalSide

object SidedHelper {
    @JvmStatic
    fun sideFromIsClient(isClient: Boolean): LogicalSide = if (isClient) LogicalSide.CLIENT else LogicalSide.SERVER
}
