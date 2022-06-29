package org.cyntho.fh.plugins

import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.cyntho.fh.database.DatabaseConnector
import org.cyntho.fh.database.DbConnector
import org.cyntho.fh.models.NetPack
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
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

        get("/db"){
            val db: DbConnector = DbConnector("localhost", 3306, "kartoffel", "wxq6V3C2dxMFX4D", "kartoffelpuffer")

            val res: ResultSet? = db.executeQuery("SELECT COUNT(*) as total FROM users", emptyArray())

            if (res == null){
                println("Something went wrong")
            } else {
                if (res.next()){
                    println("Found [${res.getInt("total")}] rows.")
                }

            }
        }


        post("/register"){
            val body = call.receive<NetPack>()
            packets.add(body)
            println("Received NetPack: $body")

            if (body.type == 0 && body.data != "" ){ // 1 := Login request

                try {
                    val db = DbConnector("localhost", 3306, "kartoffel", "wxq6V3C2dxMFX4D", "kartoffelpuffer")

                    val data = db.executeQuery("SELECT * FROM users WHERE advertiser = ?", arrayOf(body.data))
                    if (data != null){

                        // Check amount of rows returned
                        var size = 0;
                        data.last()
                        size = data.row

                        // Query successful. Load token
                        if (size == 0){

                            // Not registered yet, do it now
                            body.userToken = UUID.randomUUID().toString()
                            val inserter = db.executeQuery("INSERT INTO users SET (token, advertiser) VALUES (?, ?)", arrayOf(body.userToken, body.data))
                            println("inserter: $inserter")
                        } else {

                            // Already in there. return token
                            body.userToken = data.getString("token")

                            // update 'last login'
                            val t = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSSSSS").withZone(ZoneOffset.UTC).format(
                                Instant.now())
                            db.executeUpdate("UPDATE users SET lastlogin = ? WHERE token = ?", arrayOf(t, body.userToken))
                        }
                    }

                } catch (any: Exception){
                    any.printStackTrace()
                }
            }

            println("Response: $body")
            call.respond(body)
        }


        get("/greeting"){
            call.respondText("Das ist die Antwort!")
            println("Handling call from ${call.request}")
        }
    }
}
