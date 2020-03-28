package com.itszuvalex.itszulib.logistics

import com.itszuvalex.itszulib.TestBase
import com.itszuvalex.itszulib.api.core.Loc4

class TestLocationTracker extends TestBase {

  trait TestTracker {
    val tracker = new LocationTracker
  }

  "TestLocationTracker" should {
    "Track locations" in new TestTracker {
      val loc = Loc4(0, 0, 0, 0)
      tracker.trackLocation(loc)
      tracker.isLocationTracked(loc) shouldBe true
    }
    "Remove tracked locations" in new TestTracker {
      val loc = Loc4(0, 0, 0, 0)
      tracker.trackLocation(loc)
      tracker.isLocationTracked(loc) shouldBe true
      tracker.removeLocation(loc)
      tracker.isLocationTracked(loc) shouldBe false
    }
    "Clear" in new TestTracker {
      val loc  = Loc4(0, 0, 0, 0)
      val loc1 = Loc4(0, 0, 0, 1)
      tracker.trackLocation(loc)
      tracker.trackLocation(loc1)
      tracker.isLocationTracked(loc) shouldBe true
      tracker.isLocationTracked(loc1) shouldBe true
      tracker.clear()
      tracker.isLocationTracked(loc) shouldBe false
      tracker.isLocationTracked(loc1) shouldBe false
    }
    "clear dims" in new TestTracker {
      val loc  = Loc4(0, 0, 0, 0)
      val loc1 = Loc4(0, 0, 0, 1)
      tracker.trackLocation(loc)
      tracker.trackLocation(loc1)
      tracker.isLocationTracked(loc) shouldBe true
      tracker.isLocationTracked(loc1) shouldBe true
      tracker.clearDim(0)
      tracker.isLocationTracked(loc) shouldBe false
      tracker.isLocationTracked(loc1) shouldBe true
    }

    "Track locations by dim" in new TestTracker {
      val loc  = Loc4(0, 0, 0, 0)
      val loc1 = Loc4(0, 0, 0, 1)
      tracker.trackLocation(loc)
      tracker.trackLocation(loc1)
      val locs0 = tracker.getTrackedLocationsInDim(0)
      locs0 should contain(loc)
      locs0 shouldNot contain(loc1)
      val locs1 = tracker.getTrackedLocationsInDim(1)
      locs1 should contain(loc1)
      locs1 shouldNot contain(loc)
    }

    "efficiently track locations" should {
      "get locations in range" in new TestTracker {
        val loc = Loc4(0, 0, 0, 0)
        tracker.trackLocation(loc)
        val locs = tracker.getLocationsInRange(Loc4(0, 0, 0, 0), 25f)
        locs should contain(loc)
      }
      "not get locations out of range" in new TestTracker {
        val loc  = Loc4(0, 0, 0, 0)
        val loc1 = Loc4(0, 30, 0, 0)
        tracker.trackLocation(loc)
        tracker.trackLocation(loc1)
        val locs = tracker.getLocationsInRange(Loc4(0, 0, 0, 0), 25f)
        locs should contain(loc)
        locs shouldNot contain(loc1)
      }
      "not die on big ranges" in new TestTracker {
        val loc  = Loc4(0, 0, 0, 0)
        val loc1 = Loc4(0, 3000000, 0, 0)
        tracker.trackLocation(loc)
        tracker.trackLocation(loc1)
        var locs: Iterable[Loc4] = List()

        val thread = new Thread {
          override def run(): Unit = {
            locs = tracker.getLocationsInRange(Loc4(0, 0, 0, 0), 5000000f)
          }
        }
        thread.start()
        val timeoutMillis = 5000
        val time          = System.currentTimeMillis()
        while (thread.isAlive && ((System.currentTimeMillis() - time) < timeoutMillis)) {
          Thread.sleep(10)
        }

        val timeWaited = System.currentTimeMillis() - time
        (timeWaited < timeoutMillis) shouldBe true

        if (thread.isAlive) thread.interrupt()
        thread.join()

        locs should contain allOf(loc, loc1)
      }
      "not die on big ranges for player coords" in new TestTracker {
        val loc  = Loc4(0, 0, 0, 0)
        val loc1 = Loc4(0, 3000000, 0, 0)
        tracker.trackLocation(loc)
        tracker.trackLocation(loc1)
        var locs: Iterable[Loc4] = List()

        val thread = new Thread {
          override def run(): Unit = {
            locs = tracker.getLocationsInRange(0, (0f, 0f, 0f), 5000000f)
          }
        }
        thread.start()
        val timeoutMillis = 5000
        val time          = System.currentTimeMillis()
        while (thread.isAlive && ((System.currentTimeMillis() - time) < timeoutMillis)) {
          Thread.sleep(10)
        }

        val timeWaited = System.currentTimeMillis() - time
        (timeWaited < timeoutMillis) shouldBe true

        if (thread.isAlive) thread.interrupt()
        thread.join()

        locs should contain allOf(loc, loc1)
      }
    }
  }

}
