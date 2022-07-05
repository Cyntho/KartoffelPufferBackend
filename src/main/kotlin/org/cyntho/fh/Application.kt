package org.cyntho.fh

import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.cyntho.fh.plugins.*
import io.ktor.server.plugins.doublereceive.*

fun main() {
    embeddedServer(Netty, port = 8080, host = "0.0.0.0") {
        configureRouting()
        configureSerialization()
        install(DoubleReceive)
    }.start(wait = true)
}
