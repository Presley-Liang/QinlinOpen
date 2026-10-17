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
 * +1 is the next key (finger left), -1 the previous key (finger right).
 * The release velocity is decisive when distance did not reach its threshold,
 * particularly when a user reverses the gesture just before release.
 */
internal fun walletSwipeDirection(
  distancePx: Float,
  velocityPxPerSecond: Float,
  distanceThresholdPx: Float,
  velocityThresholdPxPerSecond: Float,
  canSwipePrevious: Boolean,
  canSwipeNext: Boolean,
): Int {
  val direction =
    when {
      abs(distancePx) >= distanceThresholdPx ->
        if (distancePx < 0f) 1 else -1
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
