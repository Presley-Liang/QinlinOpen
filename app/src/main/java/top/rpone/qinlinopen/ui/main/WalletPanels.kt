package top.rpone.qinlinopen.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import top.rpone.qinlinopen.theme.WalletThemeChoice

/** This is only the visual shell. The callbacks still use the original SMS API. */
@Composable
internal fun WalletLoginPanel(
  state: MainScreenUiState,
  onPhoneChange: (String) -> Unit,
  onCodeChange: (String) -> Unit,
  onSendCode: () -> Unit,
  onLogin: () -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  val validPhone = state.phone.matches(Regex("1\\d{10}"))
  val canLogin = validPhone && state.securityCode.isNotBlank() && !state.busy

  Card(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(28.dp),
    colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth()
        .background(
          Brush.verticalGradient(
            listOf(colors.primaryContainer.copy(alpha = 0.70f), colors.surfaceContainerLow)
          )
        ).padding(24.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text("欢迎使用我的钥匙", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
      Text(
        "通过原有短信验证登录，安全读取你已获得授权的门禁钥匙。",
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurfaceVariant,
      )

      OutlinedTextField(
        value = state.phone,
        onValueChange = onPhoneChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.busy,
        label = { Text("手机号") },
        supportingText = { Text("仅用于原有服务的账号验证") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        singleLine = true,
      )
      OutlinedTextField(
        value = state.securityCode,
        onValueChange = onCodeChange,
        modifier = Modifier.fillMaxWidth(),
        enabled = !state.busy,
        label = { Text("短信验证码") },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        singleLine = true,
      )

      OutlinedButton(
        onClick = onSendCode,
        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
        enabled = validPhone && !state.busy,
      ) { Text("发送短信验证码") }

      if (state.authErrorMessage != null) {
        Card(
          colors = CardDefaults.cardColors(containerColor = colors.errorContainer),
          modifier = Modifier.fillMaxWidth(),
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text("登录或验证码请求失败", color = colors.onErrorContainer, fontWeight = FontWeight.Medium)
            Text(
              state.authErrorMessage,
              color = colors.onErrorContainer,
              style = MaterialTheme.typography.bodySmall,
            )
          }
        }
      }

      Button(
        onClick = onLogin,
        enabled = canLogin,
        modifier = Modifier.fillMaxWidth().heightIn(min = 54.dp),
      ) {
        if (state.busy) CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        else Text("验证并获取我的钥匙")
      }
      Text(
        "只有服务器确认成功后才能开门。此界面不会模拟开门或跳过账号验证。",
        style = MaterialTheme.typography.bodySmall,
        color = colors.onSurfaceVariant,
      )
    }
  }
}

internal fun walletMaskedPhone(phone: String): String =
  if (phone.length == 11) "手机号 ···· ${phone.takeLast(4)}" else "已登录账号"

@Composable
internal fun WalletSettingsPage(
  state: MainScreenUiState,
  selectedTheme: WalletThemeChoice,
  reducedMotion: Boolean,
  onThemeChange: (WalletThemeChoice) -> Unit,
  onReducedMotionChange: (Boolean) -> Unit,
  onBack: () -> Unit,
  onRefresh: () -> Unit,
  onReorder: () -> Unit,
  onLogout: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var confirmLogout by remember { mutableStateOf(false) }
  val colors = MaterialTheme.colorScheme

  if (confirmLogout) {
    AlertDialog(
      onDismissRequest = { confirmLogout = false },
      title = { Text("确定退出登录？") },
      text = { Text("本机保存的登录会话、钥匙顺序和桌面组件绑定信息将被清除。下次使用需要重新短信登录。外观偏好会保留。") },
      confirmButton = {
        TextButton(
          enabled = !state.busy,
          onClick = {
            confirmLogout = false
            onLogout()
          },
        ) { Text("确认退出", color = colors.error) }
      },
      dismissButton = { TextButton(onClick = { confirmLogout = false }) { Text("取消") } },
    )
  }

  Column(
    modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    OutlinedButton(onClick = onBack) { Text("‹ 返回钥匙") }
    Text("设置", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)

    Card(
      colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("我的账号", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(walletMaskedPhone(state.phone), style = MaterialTheme.typography.bodyMedium)
        Text(
          "已授权钥匙：${state.doors.size} 把",
          style = MaterialTheme.typography.bodyMedium,
          color = colors.onSurfaceVariant,
        )
        OutlinedButton(onClick = onRefresh, enabled = !state.busy) {
          Text("刷新授权钥匙")
        }
        OutlinedButton(onClick = onReorder, enabled = !state.busy && state.doors.size > 1) {
          Text("调整钥匙顺序")
        }
      }
    }

    Card(
      colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("外观与交互", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
          "显示模式",
          style = MaterialTheme.typography.titleMedium,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
          WalletThemeButton("自动", WalletThemeChoice.System, selectedTheme, onThemeChange, Modifier.weight(1f))
          WalletThemeButton("浅色", WalletThemeChoice.Light, selectedTheme, onThemeChange, Modifier.weight(1f))
          WalletThemeButton("深色", WalletThemeChoice.Dark, selectedTheme, onThemeChange, Modifier.weight(1f))
        }
        HorizontalDivider()
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text("减少卡片动画", style = MaterialTheme.typography.titleMedium)
            Text(
              "关闭额外滑动弹簧，保留轻点开门",
              style = MaterialTheme.typography.bodySmall,
              color = colors.onSurfaceVariant,
            )
          }
          Spacer(Modifier.width(12.dp))
          Switch(checked = reducedMotion, onCheckedChange = onReducedMotionChange)
        }
        Text(
          "系统关闭动画时，应用也会自动减少动态效果；设置不会改变开门协议。",
          style = MaterialTheme.typography.bodySmall,
          color = colors.onSurfaceVariant,
        )
      }
    }

    Card(
      colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerLow),
      shape = RoundedCornerShape(24.dp),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("安全", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
          "钥匙权限由原有门禁服务器决定。退出登录会清除本机的会话和组件钥匙绑定。",
          style = MaterialTheme.typography.bodyMedium,
          color = colors.onSurfaceVariant,
        )
        OutlinedButton(onClick = { confirmLogout = true }, enabled = !state.busy) {
          Text("退出当前账号", color = colors.error)
        }
      }
    }
    Spacer(Modifier.height(16.dp))
  }
}

@Composable
private fun WalletThemeButton(
  title: String,
  mode: WalletThemeChoice,
  selected: WalletThemeChoice,
  onClick: (WalletThemeChoice) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (selected == mode) {
    Button(onClick = { onClick(mode) }, modifier = modifier) { Text(title) }
  } else {
    OutlinedButton(onClick = { onClick(mode) }, modifier = modifier) { Text(title) }
  }
}
