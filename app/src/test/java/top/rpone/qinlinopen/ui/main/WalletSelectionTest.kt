package top.rpone.qinlinopen.ui.main

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import top.rpone.qinlinopen.data.DoorKey

class WalletSelectionTest {
  private val first = DoorKey("c", "示例小区", "first", "东门")
  private val second = DoorKey("c", "示例小区", "second", "西门")

  @Test fun remembersExistingKey() {
    assertEquals(second.stableId, resolveSelectedDoorId(listOf(first, second), second.stableId))
  }

  @Test fun fallsBackWhenKeyNoLongerAuthorized() {
    assertEquals(first.stableId, resolveSelectedDoorId(listOf(first, second), "removed"))
  }

  @Test fun emptyListHasNoSelectedDoor() {
    assertNull(resolveSelectedDoorId(emptyList(), second.stableId))
  }
}
