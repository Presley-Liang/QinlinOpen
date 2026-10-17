package top.rpone.qinlinopen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import top.rpone.qinlinopen.theme.QinlinOpenTheme
import top.rpone.qinlinopen.theme.WalletThemeChoice
import top.rpone.qinlinopen.theme.rememberWalletUiSettings

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      val appearance = rememberWalletUiSettings()
      val systemDark = isSystemInDarkTheme()
      val useDark = when (appearance.themeChoice) {
        WalletThemeChoice.System -> systemDark
        WalletThemeChoice.Light -> false
        WalletThemeChoice.Dark -> true
      }
      QinlinOpenTheme(darkTheme = useDark) {
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
          MainNavigation()
        }
      }
    }
  }
}
