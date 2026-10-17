package top.rpone.qinlinopen.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import top.rpone.qinlinopen.data.DoorKey

class WalletPanelsTest {
  @get:Rule val rule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun loginRequiresValidPhoneAndCodeBeforeNetworkCallback() {
    var phone by mutableStateOf("")
    var code by mutableStateOf("")
    var sent = 0
    var loggedIn = 0
    rule.setContent {
      WalletLoginPanel(
        state = MainScreenUiState(phone = phone, securityCode = code),
        onPhoneChange = { phone = it },
        onCodeChange = { code = it },
        onSendCode = { sent++ },
        onLogin = { loggedIn++ },
      )
    }
    rule.onNodeWithText("发送短信验证码").assertIsNotEnabled()
    rule.onNodeWithText("验证并获取我的钥匙").assertIsNotEnabled()
    rule.onNodeWithText("手机号").performTextInput("13800138000")
    rule.onNodeWithText("短信验证码").performTextInput("123456")
    rule.onNodeWithText("发送短信验证码").assertIsEnabled().performClick()
    rule.onNodeWithText("验证并获取我的钥匙").assertIsEnabled().performClick()
    rule.runOnIdle {
      assertEquals(1, sent)
      assertEquals(1, loggedIn)
    }
  }

  @Test
  fun loginErrorAppearsInlineWithoutFakeSuccess() {
    rule.setContent {
      WalletLoginPanel(
        state = MainScreenUiState(authErrorMessage = "短信验证码无效"),
        onPhoneChange = {},
        onCodeChange = {},
        onSendCode = {},
        onLogin = {},
      )
    }
    rule.onNodeWithText("登录或验证码请求失败").assertExists()
    rule.onNodeWithText("短信验证码无效").assertExists()
  }

  @Test
  fun settingsRequireConfirmationBeforeLogout() {
    val key = DoorKey("1", "示例", "2", "东门")
    var logoutCount = 0
    rule.setContent {
      WalletSettingsPage(
        state = MainScreenUiState(
          phone = "13800138000", loggedIn = true, doors = listOf(key),
        ),
        selectedTheme = top.rpone.qinlinopen.theme.WalletThemeChoice.System,
        reducedMotion = false,
        onThemeChange = {},
        onReducedMotionChange = {},
        onBack = {},
        onRefresh = {},
        onReorder = {},
        onLogout = { logoutCount++ },
      )
    }
    rule.onNodeWithText("手机号 ···· 8000").assertExists()
    rule.onNodeWithText("退出当前账号").performClick()
    rule.onNodeWithText("取消").performClick()
    rule.runOnIdle { assertEquals(0, logoutCount) }
    rule.onNodeWithText("退出当前账号").performClick()
    rule.onNodeWithText("确认退出").performClick()
    rule.runOnIdle { assertEquals(1, logoutCount) }
  }
}
