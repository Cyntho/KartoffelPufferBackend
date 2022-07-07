package org.cyntho.fh.models

import java.sql.Timestamp

data class ReservationWrapper(var layout: Int,
                              var x: Int,
                              var y: Int,
                              var time: Long,
                              var pplCurrent: Int,
                              var pplMax: Int,
                              var dishes: MutableMap<Int, Int>?
)
