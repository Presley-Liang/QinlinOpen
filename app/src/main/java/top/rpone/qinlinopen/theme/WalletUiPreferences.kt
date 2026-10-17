package top.rpone.qinlinopen.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext

/** Cosmetic-only preferences. Authentication and widget keys remain in SecureStore. */
enum class WalletThemeChoice { System, Light, Dark }

private const val PREF_NAME = "wallet_ui_settings"
private const val THEME_KEY = "theme_mode"
private const val REDUCE_MOTION_KEY = "reduce_motion"

class WalletUiPreferences(context: Context) {
  private val preferences = context.applicationContext.getSharedPreferences(
    PREF_NAME, Context.MODE_PRIVATE,
  )

  fun themeChoice(): WalletThemeChoice =
    runCatching {
      WalletThemeChoice.valueOf(preferences.getString(THEME_KEY, null) ?: "System")
    }.getOrDefault(WalletThemeChoice.System)

  fun reducedMotion(): Boolean = preferences.getBoolean(REDUCE_MOTION_KEY, false)

  fun setThemeChoice(value: WalletThemeChoice) {
    preferences.edit().putString(THEME_KEY, value.name).apply()
  }

  fun setReducedMotion(value: Boolean) {
    preferences.edit().putBoolean(REDUCE_MOTION_KEY, value).apply()
  }

  fun observe(listener: () -> Unit): () -> Unit {
    val changeListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
      if (key == THEME_KEY || key == REDUCE_MOTION_KEY) listener()
    }
    preferences.registerOnSharedPreferenceChangeListener(changeListener)
    return { preferences.unregisterOnSharedPreferenceChangeListener(changeListener) }
  }
}

class WalletUiSettings internal constructor(
  val themeChoice: WalletThemeChoice,
  val reducedMotion: Boolean,
  val onThemeChange: (WalletThemeChoice) -> Unit,
  val onReducedMotionChange: (Boolean) -> Unit,
)

@Composable
fun rememberWalletUiSettings(): WalletUiSettings {
  val context = LocalContext.current
  val preferences = remember(context.applicationContext) {
    WalletUiPreferences(context.applicationContext)
  }
  var theme by remember(preferences) { mutableStateOf(preferences.themeChoice()) }
  var reduceMotion by remember(preferences) { mutableStateOf(preferences.reducedMotion()) }

  DisposableEffect(preferences) {
    val unregister = preferences.observe {
      theme = preferences.themeChoice()
      reduceMotion = preferences.reducedMotion()
    }
    onDispose { unregister() }
  }

  return WalletUiSettings(
    themeChoice = theme,
    reducedMotion = reduceMotion,
    onThemeChange = { value ->
      theme = value
      preferences.setThemeChoice(value)
    },
    onReducedMotionChange = { value ->
      reduceMotion = value
      preferences.setReducedMotion(value)
    },
  )
}
