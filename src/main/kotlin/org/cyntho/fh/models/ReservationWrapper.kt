package org.cyntho.fh.models

import java.sql.Timestamp


data class ReservationWrapper(
    val id: Int,
    val layout: Int,
    val x: Int,
    val y: Int,
    val time: Long
)
