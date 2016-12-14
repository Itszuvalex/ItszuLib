package com.itszuvalex.itszulib.proxy

import net.minecraftforge.fml.relauncher.Side

/**
  * Created by Christopher Harris (Itszuvalex) on 4/10/15.
  */
class ProxyServer extends ProxyCommon {
  override def side: Side = Side.SERVER
}
