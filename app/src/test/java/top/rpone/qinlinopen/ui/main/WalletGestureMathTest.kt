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
  fun boundaryReturnSpringStartsFromIdenticalDisplayedOffset() {
    val rawOnRelease = 240f
    val visibleOnRelease = walletDragOffset(rawOnRelease, false, true, 48f)
    val firstSpringFrame = walletDragOffset(rawOnRelease, false, true, 48f)
    val laterSpringFrame = walletDragOffset(180f, false, true, 48f)
    assertEquals(visibleOnRelease, firstSpringFrame, 0.001f)
    assertTrue(laterSpringFrame < firstSpringFrame)
    assertEquals(0f, walletDragOffset(0f, false, true, 48f), 0.001f)
  }

  @Test
  fun interruptingReturnSpringWithTinyDragDoesNotSelectAgain() {
    // The previous spring was still 120px left of center. New gesture moves
    // only 12px right, so inherited displacement cannot select another door.
    assertEquals(
      0,
      walletSwipeDirection(
        distancePx = -108f, velocityPxPerSecond = 150f,
        distanceThresholdPx = 72f, velocityThresholdPxPerSecond = 900f,
        canSwipePrevious = true, canSwipeNext = true,
        gestureStartOffsetPx = -120f,
      ),
    )
  }

  @Test
  fun interruptedSpringQualifiesOnlyFreshGestureDirection() {
    // Return spring is at -120px, new gesture travels 80px to the right.
    // Absolute offset is still negative; the user requested the PREVIOUS key.
    assertEquals(
      -1,
      walletSwipeDirection(
        distancePx = -40f, velocityPxPerSecond = 120f,
        distanceThresholdPx = 72f, velocityThresholdPxPerSecond = 900f,
        canSwipePrevious = true, canSwipeNext = true,
        gestureStartOffsetPx = -120f,
      ),
    )
  }

  @Test
  fun shortDragAfterInterruptStillHonorsReleaseVelocity() {
    assertEquals(
      1,
      walletSwipeDirection(
        distancePx = 70f, velocityPxPerSecond = -1450f,
        distanceThresholdPx = 72f, velocityThresholdPxPerSecond = 900f,
        canSwipePrevious = true, canSwipeNext = true,
        gestureStartOffsetPx = 110f,
      ),
    )
  }

  @Test
  fun reachingLastKeyRetainsInheritedLeftOffsetOnNextDrag() {
    // -120 px was legal on the old (penultimate) key. On the new last key,
    // it is out of bounds, but the visual offset must not be remapped.
    val inherited = -120f
    val firstFrame = walletDragFromVisibleBaseline(inherited, 0f, true, false, 48f)
    val firstOutwardMove = walletDragFromVisibleBaseline(inherited, -2f, true, false, 48f)
    val inwardMove = walletDragFromVisibleBaseline(inherited, 30f, true, false, 48f)
    assertEquals(inherited, firstFrame, 0.001f)
    // Inherited displacement beyond the new 48 px boundary remains visually
    // continuous, but is not allowed to move even farther outward.
    assertEquals(inherited, firstOutwardMove, 0.001f)
    assertEquals(-90f, inwardMove, 0.001f)
  }

  @Test
  fun reachingFirstKeyRetainsInheritedRightOffsetOnNextDrag() {
    val inherited = 120f
    val firstFrame = walletDragFromVisibleBaseline(inherited, 0f, false, true, 48f)
    val firstOutwardMove = walletDragFromVisibleBaseline(inherited, 2f, false, true, 48f)
    val inwardMove = walletDragFromVisibleBaseline(inherited, -30f, false, true, 48f)
    assertEquals(inherited, firstFrame, 0.001f)
    // Do not ratchet an inherited out-of-range position farther outward.
    assertEquals(inherited, firstOutwardMove, 0.001f)
    assertEquals(90f, inwardMove, 0.001f)
  }

  @Test
  fun normalEdgeResistanceIsUnchangedWithoutInheritedOffset() {
    assertEquals(
      walletDragOffset(-200f, true, false, 48f),
      walletDragFromVisibleBaseline(0f, -200f, true, false, 48f),
      0.001f,
    )
    assertEquals(
      walletDragOffset(200f, false, true, 48f),
      walletDragFromVisibleBaseline(0f, 200f, false, true, 48f),
      0.001f,
    )
  }

  @Test
  fun repeatedInterruptedFirstKeyRightwardDragsRemainBelowFortyEightPixels() {
    var visible = 0f
    repeat(20) {
      // A new gesture begins before the previous return spring reaches zero.
      visible = walletDragFromVisibleBaseline(visible, 300f, false, true, 48f)
      assertTrue("First key drifted outside resistance: $visible", visible in 0f..48f)
    }
    assertTrue(visible > 0f)
  }

  @Test
  fun repeatedInterruptedLastKeyLeftwardDragsRemainAboveMinusFortyEightPixels() {
    var visible = 0f
    repeat(20) {
      visible = walletDragFromVisibleBaseline(visible, -300f, true, false, 48f)
      assertTrue("Last key drifted outside resistance: $visible", visible in -48f..0f)
    }
    assertTrue(visible < 0f)
  }

  @Test
  fun draggingInwardCrossingCenterUsesOppositeBoundaryResistance() {
    // One visible baseline may be inherited from an earlier selected key.
    val pastCenterOnFirst = walletDragFromVisibleBaseline(-100f, 300f, false, true, 48f)
    val pastCenterOnLast = walletDragFromVisibleBaseline(100f, -300f, true, false, 48f)
    assertTrue(pastCenterOnFirst in 0f..48f)
    assertTrue(pastCenterOnLast in -48f..0f)
  }

  @Test
  fun interruptedEdgeReturnDoesNotJumpOnZeroMovement() {
    assertEquals(38f, walletDragFromVisibleBaseline(38f, 0f, false, true, 48f), 0.001f)
    assertEquals(-38f, walletDragFromVisibleBaseline(-38f, 0f, true, false, 48f), 0.001f)
    assertEquals(-120f, walletDragFromVisibleBaseline(-120f, 0f, true, false, 48f), 0.001f)
    assertEquals(120f, walletDragFromVisibleBaseline(120f, 0f, false, true, 48f), 0.001f)
  }

  @Test
  fun smallSlowGestureDoesNotSwitch() {
    assertEquals(0, walletSwipeDirection(35f, 350f, 72f, 900f, true, true))
  }
}
