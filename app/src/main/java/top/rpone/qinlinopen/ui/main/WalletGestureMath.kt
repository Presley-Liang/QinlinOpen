package top.rpone.qinlinopen.ui.main

import kotlin.math.abs

/**
 * Pixel-space gesture math. Callers convert dp thresholds with LocalDensity.
 *
 * A valid direction remains exactly 1:1 with the finger. At the first/last
 * key, dragging past an unavailable neighbor feels elastic and asymptotically
 * approaches [limitPx] instead of allowing the card to leave the screen.
 */
internal fun walletDragOffset(
  dragPx: Float,
  canSwipePrevious: Boolean,
  canSwipeNext: Boolean,
  limitPx: Float,
): Float {
  val outOfBounds =
    (dragPx > 0f && !canSwipePrevious) || (dragPx < 0f && !canSwipeNext)
  if (!outOfBounds) return dragPx
  if (limitPx <= 0f) return 0f
  val amount = abs(dragPx)
  val resisted = limitPx * amount / (limitPx + amount)
  return if (dragPx < 0f) -resisted else resisted
}

/**
 * Preserve the actual displayed position when interrupting a return spring.
 *
 * A repeated drag at the same boundary must not earn a fresh 48dp allowance:
 * use only the headroom left between the inherited offset and [limitPx].
 *
 * If a key change made an inherited offset exceed the new key's boundary,
 * keep that visible position continuous and block *additional* outward travel.
 * Inward travel follows the finger 1:1 until crossing zero, where the opposite
 * edge (if unavailable) becomes elastically resisted.
 */
internal fun walletDragFromVisibleBaseline(
  startVisiblePx: Float,
  gestureTravelPx: Float,
  canSwipePrevious: Boolean,
  canSwipeNext: Boolean,
  limitPx: Float,
): Float {
  val position = startVisiblePx + gestureTravelPx

  if (gestureTravelPx > 0f && !canSwipePrevious) {
    if (startVisiblePx >= 0f) {
      val headroom = (limitPx - startVisiblePx).coerceAtLeast(0f)
      return if (headroom > 0f) {
        startVisiblePx + headroom * gestureTravelPx / (headroom + gestureTravelPx)
      } else {
        startVisiblePx // Key handoff may have inherited a larger offset.
      }
    }
    if (position > 0f) {
      return walletDragOffset(position, canSwipePrevious, canSwipeNext, limitPx)
    }
  }

  if (gestureTravelPx < 0f && !canSwipeNext) {
    if (startVisiblePx <= 0f) {
      val headroom = (limitPx + startVisiblePx).coerceAtLeast(0f)
      val distance = -gestureTravelPx
      return if (headroom > 0f) {
        startVisiblePx - headroom * distance / (headroom + distance)
      } else {
        startVisiblePx // No additional travel beyond an inherited endpoint.
      }
    }
    if (position < 0f) {
      return walletDragOffset(position, canSwipePrevious, canSwipeNext, limitPx)
    }
  }

  return position
}

/**
 * +1 is the next key (finger left), -1 the previous key (finger right).
 * The release velocity is decisive when this gesture's traveled distance did
 * not reach its threshold, particularly when a user reverses before release.
 * The raw offset may include the previous interrupted spring; that inherited
 * displacement must never qualify the newly started gesture.
 */
internal fun walletSwipeDirection(
  distancePx: Float,
  velocityPxPerSecond: Float,
  distanceThresholdPx: Float,
  velocityThresholdPxPerSecond: Float,
  canSwipePrevious: Boolean,
  canSwipeNext: Boolean,
  gestureStartOffsetPx: Float = 0f,
): Int {
  val gestureTravelPx = distancePx - gestureStartOffsetPx
  val direction =
    when {
      abs(gestureTravelPx) >= distanceThresholdPx ->
        if (gestureTravelPx < 0f) 1 else -1
      abs(velocityPxPerSecond) >= velocityThresholdPxPerSecond ->
        if (velocityPxPerSecond < 0f) 1 else -1
      else -> 0
    }
  return when (direction) {
    -1 -> if (canSwipePrevious) -1 else 0
    1 -> if (canSwipeNext) 1 else 0
    else -> 0
  }
}

/**
 * The main card switches its selected key before the return spring starts.
 * Selection is synchronous but Compose's captured reachability flags still
 * describe the SOURCE key until the next recomposition. Do not allow that
 * stale reachability to launch a spring outward from the DESTINATION endpoint.
 *
 * Direction: +1 => next key, -1 => previous key, 0 => no key change.
 * All velocities are px/s; callers convert the max from dp/s first.
 */
internal fun walletSettlingVelocity(
  releaseVelocityPxPerSecond: Float,
  maxVelocityPxPerSecond: Float,
  swipeDirection: Int,
  canSwipePrevious: Boolean,
  canSwipeNext: Boolean,
  previousKeyIsFirst: Boolean,
  nextKeyIsLast: Boolean,
): Float {
  val outwardOnSource =
    (releaseVelocityPxPerSecond > 0f && !canSwipePrevious) ||
      (releaseVelocityPxPerSecond < 0f && !canSwipeNext)
  val outwardOnDestination =
    (swipeDirection == 1 && nextKeyIsLast && releaseVelocityPxPerSecond < 0f) ||
      (swipeDirection == -1 && previousKeyIsFirst && releaseVelocityPxPerSecond > 0f)
  if (outwardOnSource || outwardOnDestination) return 0f
  return releaseVelocityPxPerSecond.coerceIn(
    -maxVelocityPxPerSecond,
    maxVelocityPxPerSecond,
  )
}
