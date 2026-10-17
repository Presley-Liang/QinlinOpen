package top.rpone.qinlinopen.data

/**
 * Record a door only after the server confirms an open request.
 *
 * Used by both the app and its home-screen widgets. A preference write failure
 * must not turn a successfully completed physical door operation into an error,
 * which could prompt the user to retry an already-accepted request.
 */
internal fun openAndRememberSuccessfulDoor(
  stableId: String,
  open: () -> Unit,
  remember: (String) -> Unit,
) {
  open()
  try {
    remember(stableId)
  } catch (_: Exception) {
    // The network operation already succeeded; remember is best-effort only.
  }
}
