package top.rpone.qinlinopen.ui.main

import android.content.Context
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.provider.Settings
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import top.rpone.qinlinopen.R
import top.rpone.qinlinopen.data.DoorKey
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Respect the Android animator-duration-scale switch, including the 0x accessibility setting.
 * We intentionally use simple gradients instead of a backdrop RenderEffect: Android RenderEffect
 * blurs the composable's own layer, not reliably what lies behind it.
 */
private fun readWalletAnimatorEnabled(context: Context): Boolean =
  runCatching {
    Settings.Global.getFloat(
      context.contentResolver,
      Settings.Global.ANIMATOR_DURATION_SCALE,
      1f,
    ) > 0f
  }.getOrDefault(true)

@Composable
internal fun walletMotionEnabled(): Boolean {
  val context = LocalContext.current
  val owner = LocalLifecycleOwner.current
  var enabled by remember(context) { mutableStateOf(readWalletAnimatorEnabled(context)) }

  DisposableEffect(context, owner) {
    val resolver = context.contentResolver
    fun refresh() { enabled = readWalletAnimatorEnabled(context) }
    val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
      override fun onChange(selfChange: Boolean) { refresh() }
    }
    val lifecycleObserver = LifecycleEventObserver { _, event ->
      if (event == Lifecycle.Event.ON_RESUME) refresh()
    }
    owner.lifecycle.addObserver(lifecycleObserver)
    resolver.registerContentObserver(
      Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE),
      false,
      observer,
    )
    refresh()
    onDispose {
      resolver.unregisterContentObserver(observer)
      owner.lifecycle.removeObserver(lifecycleObserver)
    }
  }
  return enabled
}

@Composable
internal fun WalletMainCard(
  key: DoorKey,
  enabled: Boolean,
  visual: DoorKeyVisual,
  errorMessage: String? = null,
  canSwipePrevious: Boolean,
  canSwipeNext: Boolean,
  motionEnabled: Boolean,
  onClick: () -> Unit,
  onSwipe: (Int) -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  val haptics = LocalHapticFeedback.current
  val scope = rememberCoroutineScope()
  val density = LocalDensity.current
  val switchDistance = with(density) { 72.dp.toPx() }
  val switchVelocity = with(density) { 900.dp.toPx() }
  val maxSettleVelocity = with(density) { 1800.dp.toPx() }
  val edgeLimit = with(density) { 48.dp.toPx() }
  val motion = motionEnabled
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (motion && pressed && enabled) 0.975f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
    label = "wallet-press-scale",
  )
  val successScale by animateFloatAsState(
    targetValue = if (motion && visual == DoorKeyVisual.Success) 1.012f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    label = "wallet-success-settle",
  )
  var offsetPx by remember { mutableFloatStateOf(0f) }
  // Capture the visible position once per gesture, not the inherited raw spring
  // distance: it may have been legal on the previous key but illegal on this one.
  var dragStartVisiblePx by remember { mutableFloatStateOf(0f) }
  var gestureTravelPx by remember { mutableFloatStateOf(0f) }
  var settleJob by remember { mutableStateOf<Job?>(null) }
  var suppressOpenUntil by remember { mutableLongStateOf(0L) }
  val dragState = rememberDraggableState { delta ->
    gestureTravelPx += delta
    offsetPx = walletDragFromVisibleBaseline(
      startVisiblePx = dragStartVisiblePx,
      gestureTravelPx = gestureTravelPx,
      canSwipePrevious = canSwipePrevious,
      canSwipeNext = canSwipeNext,
      limitPx = edgeLimit,
    )
  }
  LaunchedEffect(motion) {
    if (!motion) {
      settleJob?.cancel()
      gestureTravelPx = 0f
      dragStartVisiblePx = 0f
      offsetPx = 0f
    }
  }
  LaunchedEffect(visual, key.stableId) {
    if (visual == DoorKeyVisual.Success) {
      haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    }
  }
  val shape = RoundedCornerShape(28.dp)
  val borderColor = if (visual == DoorKeyVisual.Success) colors.tertiary else colors.outline.copy(alpha = 0.40f)

  Card(
    onClick = {
      if (enabled && SystemClock.uptimeMillis() >= suppressOpenUntil) {
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onClick()
      }
    },
    enabled = enabled,
    interactionSource = interaction,
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 222.dp)
      .testTag("wallet-main-card")
      .graphicsLayer {
        translationX = offsetPx
        scaleX = scale * successScale
        scaleY = scale * successScale
      }
      .draggable(
        state = dragState,
        orientation = Orientation.Horizontal,
        enabled = enabled && motion,
        onDragStarted = {
          suppressOpenUntil = SystemClock.uptimeMillis() + 1_000L
          settleJob?.cancel()
          // Keep the exact visible position after a key change; any edge
          // resistance belongs to the NEW finger movement only.
          dragStartVisiblePx = offsetPx
          gestureTravelPx = 0f
        },
        onDragStopped = { velocity ->
          suppressOpenUntil = SystemClock.uptimeMillis() + 250L
          val oldVisibleOffset = offsetPx
          // Distance wins only if it crossed the full selection threshold.
          // For a shorter gesture reversed at release, velocity determines direction.
          val direction = walletSwipeDirection(
            distancePx = gestureTravelPx,
            velocityPxPerSecond = velocity,
            distanceThresholdPx = switchDistance,
            velocityThresholdPxPerSecond = switchVelocity,
            canSwipePrevious = canSwipePrevious,
            canSwipeNext = canSwipeNext,
          )
          if (direction != 0) {
            onSwipe(direction)
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
          }
          val settlingVelocity =
            if ((velocity > 0f && !canSwipePrevious) || (velocity < 0f && !canSwipeNext)) 0f
            else velocity.coerceIn(-maxSettleVelocity, maxSettleVelocity)
          settleJob?.cancel()
          settleJob = scope.launch {
            animate(
              initialValue = oldVisibleOffset,
              targetValue = 0f,
              initialVelocity = settlingVelocity,
              animationSpec = spring(
                dampingRatio = Spring.DampingRatioNoBouncy,
                stiffness = Spring.StiffnessMediumLow,
              ),
            ) { value, _ ->
              // Animate the display-space offset itself. Reapplying the current
              // key's edge curve here would snap a returning card after swiping
              // onto a key with a different neighbor configuration.
              offsetPx = value
            }
            gestureTravelPx = 0f
            dragStartVisiblePx = 0f
            offsetPx = 0f
          }
        },
      ),
    shape = shape,
    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
    border = BorderStroke(1.dp, borderColor),
  ) {
    // Bounded glass-style surface: edge highlights and translucent layered tints.
    // Avoid per-frame blur allocations and preserve readable solid text.
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .clip(shape)
        .background(
          Brush.linearGradient(
            listOf(
              colors.surfaceContainerLowest,
              colors.surfaceContainerLow,
              colors.primaryContainer,
            )
          )
        )
        .background(
          Brush.verticalGradient(
            listOf(
              Color.White.copy(alpha = if (colors.background.luminance() > 0.5f) 0.50f else 0.10f),
              Color.Transparent,
              colors.primary.copy(alpha = 0.07f),
            )
          )
        )
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().heightIn(min = 222.dp).padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.Top,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("数字门禁钥匙", style = MaterialTheme.typography.labelLarge, color = colors.onSurfaceVariant)
            Spacer(Modifier.size(1.dp, 12.dp))
            Text(key.doorName, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            if (key.communityName.isNotBlank()) {
              Text(key.communityName, style = MaterialTheme.typography.bodySmall, color = colors.onSurfaceVariant)
            }
          }
          Box(
            modifier = Modifier.size(52.dp)
              .clip(RoundedCornerShape(18.dp))
              .background(colors.primary.copy(alpha = 0.12f))
              .border(1.dp, colors.onSurface.copy(alpha = 0.08f), RoundedCornerShape(18.dp)),
            contentAlignment = Alignment.Center,
          ) {
            when (visual) {
              DoorKeyVisual.Normal -> Icon(
                painter = painterResource(R.drawable.ic_lock_open),
                contentDescription = null, tint = colors.primary,
              )
              DoorKeyVisual.Loading -> CircularProgressIndicator(modifier = Modifier.size(26.dp), strokeWidth = 2.dp)
              DoorKeyVisual.Success -> Icon(
                painter = painterResource(R.drawable.ic_check_circle),
                contentDescription = null, tint = colors.primary,
              )
            }
          }
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            when {
              visual == DoorKeyVisual.Loading -> "正在发送开门请求…"
              visual == DoorKeyVisual.Success -> "开门指令已被服务器接受"
              errorMessage != null -> "请求失败，请轻点卡片重试"
              else -> "轻点开门 · 左右滑动切换",
            },
            style = MaterialTheme.typography.bodyMedium,
            color = if (errorMessage != null) colors.error else colors.onSurfaceVariant,
            modifier = Modifier.weight(1f),
          )
          Spacer(Modifier.width(10.dp))
          Box(
            modifier = Modifier.size(46.dp).clip(RoundedCornerShape(23.dp)).background(colors.primary),
            contentAlignment = Alignment.Center,
          ) {
            Icon(
              painter = painterResource(R.drawable.ic_lock_open),
              contentDescription = null, tint = colors.onPrimary, modifier = Modifier.size(24.dp),
            )
          }
        }
        if (errorMessage != null) {
          Text(
            text = errorMessage,
            style = MaterialTheme.typography.bodySmall,
            color = colors.error,
          )
        }
      }
    }
  }
}

@Composable
internal fun WalletOtherCard(
  key: DoorKey,
  index: Int,
  enabled: Boolean,
  motionEnabled: Boolean,
  onClick: () -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  val haptics = LocalHapticFeedback.current
  val motion = motionEnabled
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val scale by animateFloatAsState(
    targetValue = if (motion && pressed && enabled) 0.985f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessHigh),
    label = "wallet-other-press",
  )
  val shape = RoundedCornerShape(22.dp)
  Card(
    onClick = {
      haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
      onClick()
    },
    enabled = enabled,
    interactionSource = interaction,
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = ((index % 3) * 4).dp)
      .heightIn(min = 72.dp)
      .graphicsLayer { scaleX = scale; scaleY = scale },
    shape = shape,
    border = BorderStroke(1.dp, colors.outline.copy(alpha = 0.22f)),
    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainer),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)
        .background(
          Brush.horizontalGradient(listOf(colors.surfaceContainer, colors.primaryContainer.copy(alpha = 0.55f)))
        )
        .padding(horizontal = 20.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(painter = painterResource(R.drawable.ic_lock_open), contentDescription = null, tint = colors.primary)
      Spacer(Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(key.doorName, fontWeight = FontWeight.Medium)
        if (key.communityName.isNotBlank()) {
          Text(key.communityName, color = colors.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
        }
      }
      Text("选择", color = colors.primary, style = MaterialTheme.typography.labelMedium)
    }
  }
}
