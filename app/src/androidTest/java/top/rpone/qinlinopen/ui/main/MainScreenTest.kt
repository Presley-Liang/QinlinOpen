package top.rpone.qinlinopen.ui.main

import androidx.activity.ComponentActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertIsDisplayed
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
  @Test
  fun shrinkingToOneDoorExitsSortModeAndAllowsOpening() {
    val first = DoorKey("1", "示例小区", "2", "东门")
    val second = DoorKey("1", "示例小区", "3", "西门")
    var keys by mutableStateOf(listOf(first, second))
    var opened: DoorKey? = null
    composeTestRule.setContent {
      MainScreen(
        state = MainScreenUiState(loggedIn = true, doors = keys),
        onPhoneChange = {}, onCodeChange = {}, onSendCode = {}, onLogin = {},
        onRefresh = {}, onOpenDoor = { opened = it }, onSelectDoor = {},
        onMoveDoor = { _, _ -> }, onDoorOrderChanged = {}, onLogout = {},
      )
    }
    composeTestRule.onNodeWithText("管理钥匙顺序").performClick()
    composeTestRule.onNodeWithText("完成排序").assertIsDisplayed()
    composeTestRule.runOnIdle { keys = listOf(first) }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("完成排序").assertDoesNotExist()
    composeTestRule.onNodeWithText("轻点卡片开门").assertIsDisplayed()
    composeTestRule.onNodeWithText("东门").performClick()
    composeTestRule.runOnIdle { assertEquals(first, opened) }
  }

  @Test
  fun shrinkingToZeroDoorsExitsSortModeAndShowsEmptyState() {
    val first = DoorKey("1", "示例小区", "2", "东门")
    val second = DoorKey("1", "示例小区", "3", "西门")
    var keys by mutableStateOf(listOf(first, second))
    composeTestRule.setContent {
      MainScreen(
        state = MainScreenUiState(loggedIn = true, doors = keys),
        onPhoneChange = {}, onCodeChange = {}, onSendCode = {}, onLogin = {},
        onRefresh = {}, onOpenDoor = {}, onSelectDoor = {},
        onMoveDoor = { _, _ -> }, onDoorOrderChanged = {}, onLogout = {},
      )
    }
    composeTestRule.onNodeWithText("管理钥匙顺序").performClick()
    composeTestRule.runOnIdle { keys = emptyList() }
    composeTestRule.waitForIdle()
    composeTestRule.onNodeWithText("完成排序").assertDoesNotExist()
    composeTestRule.onNodeWithText("当前没有可用钥匙").assertExists()
  }

}
