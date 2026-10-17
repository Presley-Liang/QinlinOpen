package top.rpone.qinlinopen.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class DoorOpenHistoryTest {
  @Test
  fun mainScreenSuccessfulOpenRemembersThatKey() {
    var openCalls = 0
    var savedKey: String? = null

    openAndRememberSuccessfulDoor(
      stableId = "community:front",
      open = { openCalls++ },
      remember = { savedKey = it },
    )

    assertEquals(1, openCalls)
    assertEquals("community:front", savedKey)
  }

  @Test
  fun widgetSuccessfulOpenRemembersItsOwnBoundKey() {
    val lastOpened = mutableListOf<String>()

    openAndRememberSuccessfulDoor(
      stableId = "community:widget-door",
      open = { /* Server-confirmed success */ },
      remember = { lastOpened += it },
    )

    assertEquals(listOf("community:widget-door"), lastOpened)
  }

  @Test
  fun serverFailureNeverRecordsLastOpenedKey() {
    var rememberCalled = false

    assertThrows(IllegalStateException::class.java) {
      openAndRememberSuccessfulDoor(
        stableId = "community:failed",
        open = { error("server rejected open") },
        remember = { rememberCalled = true },
      )
    }

    assertEquals(false, rememberCalled)
  }

  @Test
  fun encryptedPreferenceFailureDoesNotMasqueradeAsDoorFailure() {
    var opened = false

    openAndRememberSuccessfulDoor(
      stableId = "community:success",
      open = { opened = true },
      remember = { throw IllegalStateException("Keystore temporarily unavailable") },
    )

    assertTrue(opened)
  }
}
