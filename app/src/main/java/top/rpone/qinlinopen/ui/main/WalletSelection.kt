package top.rpone.qinlinopen.ui.main

import top.rpone.qinlinopen.data.DoorKey

/** A saved selection is only valid while the server still grants that key. */
internal fun resolveSelectedDoorId(doors: List<DoorKey>, preferredId: String?): String? =
  preferredId?.takeIf { id -> doors.any { it.stableId == id } } ?: doors.firstOrNull()?.stableId
