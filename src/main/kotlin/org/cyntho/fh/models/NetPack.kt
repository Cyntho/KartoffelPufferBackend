package org.cyntho.fh.models

@kotlinx.serialization.Serializable
data class NetPack(
    val time: Long,
    var userToken: String,
    var type: Int,
    var data: String
)
