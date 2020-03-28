package com.itszuvalex.itszulib.api.wrappers

import com.itszuvalex.itszulib.TestBase

/**
  * Created by Christopher Harris (Itszuvalex) on 7/14/16.
  */
class TestWrapperVanillaItemStack extends TestBase {
  /**
    * Can no longer wrap actual itemstacks, nor can I stub the class out, due to forge requiring items to be registered.
    */

  "a vanilla wrapper" when {
    "wrapping null" should {
      "wrap" in {
        WrapperVanillaItemStack(null)
      }
      "be empty" in {
        val wrap = WrapperVanillaItemStack(null)
        wrap shouldBe 'Empty
      }
    }
  }
}
