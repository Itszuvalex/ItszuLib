package com.itszuvalex.itszulib.api.access

import com.itszuvalex.itszulib.TestBase

/**
  * Created by Christopher Harris (Itszuvalex) on 3/10/2016.
  */
class TestArrayItemCollectionAccess extends TestBase {
  "An ArrayItemCollectionAccess" when {
    "constructed on an empty array" should {
      "have length equal to the backing array" in new AccessHelpers.PartialArray {
        val collection = new ArrayItemCollectionAccess(array)
        collection.length shouldEqual array.length
      }
    }
  }
}
