package top.rpone.qinlinopen.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import top.rpone.qinlinopen.data.DoorKey

class MainScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  @Test
  fun tappingSingleForegroundCardOpensThatDoor() {
    val key = DoorKey("1", "示例小区", "2", "示例门")
    var openedKey: DoorKey? = null
    composeTestRule.setContent {
      MainScreen(
        state = MainScreenUiState(loggedIn = true, doors = listOf(key)),
        onPhoneChange = {},
        onCodeChange = {},
        onSendCode = {},
        onLogin = {},
        onRefresh = {},
        onOpenDoor = { openedKey = it },
        onSelectDoor = {},
        onMoveDoor = { _, _ -> },
        onDoorOrderChanged = {},
        onLogout = {},
      )
    }
    composeTestRule.onNodeWithText("示例门").assertExists().performClick()
    composeTestRule.onNodeWithText("示例小区").assertExists()
    composeTestRule.runOnIdle { assertEquals(key, openedKey) }
  }

  @Test
  fun tappingBackgroundCardSelectsWithoutSendingOpenRequest() {
    val first = DoorKey("1", "示例小区", "2", "东门")
    val second = DoorKey("1", "示例小区", "3", "西门")
    var selected by mutableStateOf(first.stableId)
    var opened: DoorKey? = null
    composeTestRule.setContent {
      MainScreen(
        state = MainScreenUiState(loggedIn = true, doors = listOf(first, second), selectedDoorId = selected),
        onPhoneChange = {},
        onCodeChange = {},
        onSendCode = {},
        onLogin = {},
        onRefresh = {},
        onOpenDoor = { opened = it },
        onSelectDoor = { selected = it },
        onMoveDoor = { _, _ -> },
        onDoorOrderChanged = {},
        onLogout = {},
      )
    }
    composeTestRule.onNodeWithText("西门").performClick()
    composeTestRule.runOnIdle {
      assertEquals(second.stableId, selected)
      assertNull(opened)
    }
    composeTestRule.onNodeWithText("西门").performClick()
    composeTestRule.runOnIdle { assertEquals(second, opened) }
  }
}
