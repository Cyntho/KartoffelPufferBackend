package org.cyntho.fh.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.cyntho.fh.models.NetPack
import java.util.*

fun Application.configureSerialization() {
    install(ContentNegotiation) {
        json()
    }

    val packets = mutableListOf<NetPack>()

    routing {
        get("/json/kotlinx-serialization") {
            call.respond(mapOf("hello" to "world"))
        }


        post("/register"){
            val body = call.receive<NetPack>()
            packets.add(body)
            println("Received NetPack: $body")
            body.userToken = UUID.randomUUID().toString()
            println("Response: $body")
            call.respond(body)
        }


        get("/greeting"){
            call.respondText("Das ist die Antwort!")
            println("Handling call from ${call.request}")
        }
    }
}
