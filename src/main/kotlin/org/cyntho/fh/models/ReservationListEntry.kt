package org.cyntho.fh.models

import java.sql.Timestamp

data class ReservationListEntry(
    val id: Int,
    val layout: Int,
    val start: Long,
    val end: Long,
    val username: String,
    val people: Int,
)
