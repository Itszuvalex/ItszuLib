package com.itszuvalex.itszulib

/**
  * Created by Christopher Harris (Itszuvalex) on 4/11/15.
  */
class TestHelloWorld extends TestBase {

  "A test" when {
    "running" should {
      "run" in {
        System.out.println("Hello World")
      }
    }
  }

}
