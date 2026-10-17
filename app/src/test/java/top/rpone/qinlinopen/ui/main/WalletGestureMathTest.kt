package top.rpone.qinlinopen.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WalletGestureMathTest {
  @Test
  fun interiorDragFollowsFingerExactly() {
    assertEquals(180f, walletDragOffset(180f, true, true, 48f), 0.001f)
    assertEquals(-180f, walletDragOffset(-180f, true, true, 48f), 0.001f)
  }

  @Test
  fun firstKeyRightwardDragHasIncreasingResistance() {
    val first = walletDragOffset(24f, false, true, 48f)
    val second = walletDragOffset(240f, false, true, 48f)
    assertTrue(first > 0f && first < 24f)
    assertTrue(second > first && second < 48f)
    assertEquals(-100f, walletDragOffset(-100f, false, true, 48f), 0.001f)
  }

  @Test
  fun lastKeyLeftwardDragHasIncreasingResistance() {
    val first = walletDragOffset(-24f, true, false, 48f)
    val second = walletDragOffset(-240f, true, false, 48f)
    assertTrue(first < 0f && first > -24f)
    assertTrue(second < first && second > -48f)
    assertEquals(100f, walletDragOffset(100f, true, false, 48f), 0.001f)
  }

  @Test
  fun noNeighborsResistsInBothDirections() {
    assertTrue(walletDragOffset(500f, false, false, 48f) < 48f)
    assertTrue(walletDragOffset(-500f, false, false, 48f) > -48f)
  }

  @Test
  fun reversedFlickUsesReleaseVelocityUnderDistanceThreshold() {
    assertEquals(
      1,
      walletSwipeDirection(
        distancePx = 30f, velocityPxPerSecond = -1800f,
        distanceThresholdPx = 72f, velocityThresholdPxPerSecond = 900f,
        canSwipePrevious = true, canSwipeNext = true,
      ),
    )
    assertEquals(
      -1,
      walletSwipeDirection(
        distancePx = -30f, velocityPxPerSecond = 1800f,
        distanceThresholdPx = 72f, velocityThresholdPxPerSecond = 900f,
        canSwipePrevious = true, canSwipeNext = true,
      ),
    )
  }

  @Test
  fun qualifyingLongDragWinsOverOppositeReleaseVelocity() {
    assertEquals(
      -1,
      walletSwipeDirection(90f, -2000f, 72f, 900f, true, true),
    )
    assertEquals(
      1,
      walletSwipeDirection(-90f, 2000f, 72f, 900f, true, true),
    )
  }

  @Test
  fun unavailableNeighborNeverTriggersSelection() {
    assertEquals(0, walletSwipeDirection(150f, 2000f, 72f, 900f, false, true))
    assertEquals(0, walletSwipeDirection(-150f, -2000f, 72f, 900f, true, false))
    assertEquals(0, walletSwipeDirection(-150f, -2000f, 72f, 900f, false, false))
  }

  @Test
  fun smallSlowGestureDoesNotSwitch() {
    assertEquals(0, walletSwipeDirection(35f, 350f, 72f, 900f, true, true))
  }
}
