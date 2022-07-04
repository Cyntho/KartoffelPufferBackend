package org.cyntho.fh.plugins

import com.google.gson.GsonBuilder
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import kotlinx.serialization.json.*
import org.cyntho.fh.database.DbConnector
import org.cyntho.fh.models.Array2D
import org.cyntho.fh.models.LayoutWrapper
import org.cyntho.fh.models.NetPack
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
    val db = DbConnector("localhost", 3306, "kartoffel", "wxq6V3C2dxMFX4D", "kartoffelpuffer")

    routing {

        post("/register"){
            val body = call.receive<NetPack>()
            packets.add(body)
            println("Received NetPack: $body")

            if (body.type == 1 && body.data != "" ){ // 1 := Registration request

                try {
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
                            db.executeUpdate("INSERT INTO users (token, advertiser) VALUES (?, ?)", arrayOf(body.userToken, body.data))
                        } else {

                            // Already in there. return token
                            body.userToken = data.getString("token")

                            val isAdmin = data.getBoolean("isAdmin")
                            body.data = isAdmin.toString()

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


            body.type = 0 // Status Message
            println("Response: $body")
            call.respond(body)
        }

        post("/auth"){
            val body = call.receive<NetPack>()
            println("Received NetPack: $body")
            var isAdmin = false
            var userId = -1
            val code = body.data

            if (body.type == 2 && code.length == 7){ // 2 := login request

                val rs = db.executeQuery("SELECT * FROM users WHERE token = ? LIMIT 1", arrayOf(body.userToken))
                if (rs != null && rs.next()){
                    userId = rs.getInt("id")
                    isAdmin = rs.getBoolean("isAdmin")

                    if (isAdmin){
                        println("User [${body.userToken}] already set as admin")
                    }
                }

                if (!isAdmin){
                    val authReq = db.executeQuery("SELECT * FROM admin_tokens WHERE code = ? AND used_by IS NULL", arrayOf(code))
                    if (authReq != null && authReq.next()){
                        // code valid, update database

                        db.executeUpdate("UPDATE admin_tokens SET used_by = ? WHERE id = ?", arrayOf(userId, authReq.getInt("id")))
                        db.executeUpdate("UPDATE users SET isAdmin = ? WHERE token = ?", arrayOf(true, body.userToken))

                        println("User [${body.userToken}] successfully authenticated as admin using code [$code]")
                        isAdmin = true
                    }
                }
            }

            if (isAdmin){
                body.data = "AUTH_SUCCESSFUL"
            } else {
                body.data = "AUTH_FAILED"
                println("Authentication failure: User [${body.userToken}] attempted auth using invalid code: [$code]")
            }


            body.type = 0 // turn into status message
            call.respond(body)
        }

        post("/getCurrentLayout"){
            try {
                val body = call.receive<NetPack>()
                val layoutRS = db.executeQuery("SELECT * FROM layouts WHERE active = ? AND validFrom < ? ORDER BY validFrom DESC",
                    arrayOf(true, Timestamp(System.currentTimeMillis())))

                if (layoutRS != null){
                    if (layoutRS.next()){

                        var raw = layoutRS.getString("data")

                        val wrapper = LayoutWrapper(layoutRS.getInt("id"),
                                                    layoutRS.getInt("size_x"),
                                                    layoutRS.getInt("size_y"),
                                                    layoutRS.getString("name"),
                                                    layoutRS.getTimestamp("created").time,
                                                    layoutRS.getTimestamp("validFrom").time,
                                                    layoutRS.getBoolean("active"), null)
                        // Some weird work here..
                        wrapper.fillFromString(raw)

                        body.data = GsonBuilder().create().toJson(wrapper)
                        body.type = 0
                    }
                }
                call.respond(body)
            } catch (any: Exception){
                any.printStackTrace()
            }
        }

        post("/updateLayout"){
            val body = call.receive<NetPack>()
            println("Received raw packet: $body")
            try {
                val gson    = GsonBuilder().create()
                val wrapper = gson.fromJson(body.data, LayoutWrapper::class.java)

                val arr = wrapper.asArray2D()
                arr?.prettyPrint()

                val authReq = db.executeQuery("SELECT COUNT(*) as total FROM users WHERE token = ? AND isAdmin = ?", arrayOf(body.userToken, true))

                if (authReq != null && authReq.next()){

                    // User is not authorized to do this
                    if (authReq.getInt("total") == 0){
                        body.type = 0
                        body.data = "ERR_AUTH"
                        call.respond(body)
                        println("Unauthorized call to /updateLayout")
                        return@post
                    }

                    val fetchReq = db.executeQuery("SELECT COUNT(*) as total FROM layouts WHERE id = ?", arrayOf(wrapper.id))

                    // Layout exists already, needs to be updated
                    var hasNext = false
                    var counter = -1
                    if (fetchReq != null){
                        hasNext = fetchReq.next()
                        counter = fetchReq.getInt("total")
                    }

                    if (fetchReq != null && hasNext && counter == 1){

                        val update = db.executeUpdate("UPDATE layouts SET " +
                                "size_x = ?, " +
                                "size_y = ?, " +
                                "data = ?, " +
                                "name = ?, " +
                                "created = ?, " +
                                "validFrom = ?, " +
                                "active = ? WHERE id = ?",

                        arrayOf(
                            wrapper.sizeX,
                            wrapper.sizeY,
                            //Json.encodeToJsonElement(wrapper.data.arrayContents).toString(),
                            //GsonBuilder().create().fromJson(wrapper.data, Array2D::class.java),
                            wrapper.data.toString(),
                            wrapper.name,
                            Timestamp(wrapper.created),
                            Timestamp(wrapper.validFrom),
                            wrapper.active,
                            wrapper.id))

                        if (update == 1){
                            println("User [${body.userToken}] updated layout '${wrapper.name}' [id: ${wrapper.id}]")
                            body.data = "ERR_SUCCESS"
                        }
                    } else {
                        val insert = db.executeUpdate("INSERT INTO layouts (size_x, size_y, data, name, created, validFrom, active) VALUES (?, ?, ?, ?, ?, ?, ?)",
                        arrayOf(
                            wrapper.sizeX,
                            wrapper.sizeY,
                            wrapper.data.toString(),
                            wrapper.name,
                            Timestamp(wrapper.created),
                            Timestamp(wrapper.validFrom),
                            wrapper.active
                        ))
                        println("User [${body.userToken}] created layout '${wrapper.name}' [id: ${wrapper.id}]")
                    }
                }

            } catch (any: java.lang.Exception){
                any.printStackTrace()
            }

            body.type = 0
            call.respond(body)
        }
    }
}
