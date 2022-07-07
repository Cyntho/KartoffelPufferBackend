package org.cyntho.fh.plugins

import com.google.gson.GsonBuilder
import io.ktor.serialization.kotlinx.json.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.application.*
import io.ktor.server.http.content.*
import io.ktor.server.response.*
import io.ktor.server.request.*
import io.ktor.server.routing.*
import org.cyntho.fh.database.DbConnector
import org.cyntho.fh.models.*
import java.io.File
import java.sql.ResultSet
import java.sql.SQLException
import java.sql.Timestamp
import java.text.DateFormat
import java.text.SimpleDateFormat
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

    fun authUserAsGuestOrAdmin(token: String): Boolean{
        val rs = db.executeQuery("SELECT COUNT(*) as total FROM users WHERE token = ?", arrayOf(token))
        return (rs != null && rs.next() && rs.getInt("total") == 1)
    }

    fun authUserAsAdmin(token: String): Boolean{
        val rs = db.executeQuery("SELECT COUNT(*) as total FROM users WHERE token = ? AND isAdmin = ?", arrayOf(token, true))
        return (rs != null && rs.next() && rs.getInt("total") == 1)
    }

    fun getUserID(token: String): Int {
        val rs = db.executeQuery("SELECT id FROM users WHERE token = ?", arrayOf(token))
        if (rs != null && rs.next()){
            return rs.getInt("id")
        }
        return -1
    }

    fun isSameDay(first: Date, second: Date): Boolean {
        val sf = SimpleDateFormat("yyyyMMdd")

        val retVal = sf.format(first).equals(sf.format(second))

        println("[${sf.format(first)}].equals(${sf.format(second)}) ==> $retVal")

        return retVal
    }

    fun getLayoutIdFor(time: Timestamp): Int {

        val rs = db.executeQuery("SELECT * FROM layouts WHERE active = ? " +
                "AND validFrom <= ? ORDER BY validFROM DESC",
                arrayOf(
                    true,
                    time
                ))

        return -1
    }

    routing {

        static ("/static_dishes"){
            staticRootFolder = File("img/dishes")
            files(".")
            default("file_not_found.png")
            defaultResource("file_not_found.png")
            println("Routing dishes..")
        }
        static ("/static_allergies"){
            staticRootFolder = File("img/")
            files(".")
            default("file_not_found.png")
            defaultResource("file_not_found.png")
            println("Routing allergies..")
        }

        get("/test"){
            val now = System.currentTimeMillis()
            val ts = Timestamp(now)

            val heute = SimpleDateFormat("yyyy-mm-dd").format(ts)

            val endeDesTages = Timestamp.valueOf("$heute 23:59:59")

            println("heute is: $endeDesTages")

        }

        post("/getImprint"){
            try {
                val body = call.receive<NetPack>()
                val imprintData = File("data/imprint.txt").readLines()

                body.data = imprintData.toString()
                call.respond(body)
            } catch (ex: ContentTransformationException){
                println("Received invalid packet at '/getImprint'")
            }
        }

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
                            db.executeUpdate("INSERT INTO users (token, advertiser, username) VALUES (?, ?, ?)", arrayOf(body.userToken, body.data, "Unknown Guest"))
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

                var desiredTime: Timestamp? = null
                if (body.data == ""){
                    desiredTime = Timestamp(System.currentTimeMillis())
                } else {
                    desiredTime = GsonBuilder().create().fromJson(body.data, Timestamp::class.java)
                }

                val layoutRS = db.executeQuery("SELECT * FROM layouts WHERE active = ? AND validFrom < ? ORDER BY validFrom DESC",
                    arrayOf(true, desiredTime!!))

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
            } catch (ex: ContentTransformationException){
                println("Received invalid packed on /getCurrentLayout")
            }
        }

        post("/getReservationsFor"){
            try {
                val body = call.receive<NetPack>()
                println("Received NetPack: $body")
                if (!authUserAsGuestOrAdmin(body.userToken)){
                    println("Unauthorized request")
                    body.type = -1
                    call.respond(body)
                    return@post
                }

                val rs = db.executeQuery("SELECT * FROM reservations WHERE layout = ? AND appointment_end > ?",
                arrayOf(body.type, Timestamp(body.data.toLong())))

                val reservations = mutableListOf<ReservationHolder>()
                var counter = 0

                if (rs != null){
                    while (rs.next()){
                        reservations.add(counter++, ReservationHolder(
                            rs.getInt("id"),
                            rs.getInt("layout"),
                            rs.getInt("pos_x"),
                            rs.getInt("pos_y"),
                            rs.getTimestamp("appointment_end").time)
                        )
                    }
                }

                println("Reservations:")
                println("$reservations")

                body.data = GsonBuilder().create().toJson(reservations)
                body.type = 13372
                println("Response: $body")
                call.respond(body)

            } catch (ex: ContentTransformationException){
                println("Received invalid packed on /getReservationFor")
            } catch (sql: SQLException){
                println("Sql: ${sql.message}")
            }
        }

        post("/updateLayout"){
            val body = call.receive<NetPack>()
            println("Received raw packet: $body")
            try {
                val gson    = GsonBuilder().create()
                val wrapper = gson.fromJson(body.data, LayoutWrapper::class.java)
                body.type = 1

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

        post("/deleteLayout"){

        }

        post("/setUsername") {
            try {
                val body = call.receive<NetPack>()
                if (getUserID(body.userToken) == -1) {
                    body.type = -1
                    println("Received unauthorized request at '/attempReservation'")
                    return@post
                }
                db.executeUpdate("UPDATE users SET username = ? WHERE token = ?", arrayOf(body.data,body.userToken))
                call.respond(body)
            }
            catch (ex: ContentTransformationException) {
                println("Received invalid packet at '/updateUsername'")
            }
        }
        post("/getAllergyList") {
            try {
                val body = call.receive<NetPack>()
                val list = mutableListOf<AllergyWrapper>()

                println("Received: $body")

                val qry = db.executeQuery("SELECT * FROM allergy_list", arrayOf())
                if (qry != null){
                    while (qry.next()){
                        list.add(list.size, AllergyWrapper(
                            qry.getInt("id"),
                            qry.getString("name"),
                            qry.getString("iconLink"))
                        )
                    }
                }

                body.type = 0
                body.data = GsonBuilder().create().toJson(list)
                call.respond(body)

                println("Response: $body")
            } catch (ex: ContentTransformationException){
                println("Received invalid packet at '/getAllergies'")
            }
        }

        post("/getDishes"){
            try {
                val body = call.receive<NetPack>()
                val list = mutableListOf<Dish>()

                println("Received: $body")
                body.type = -1

                val stmt = "SELECT  DISTINCT\n" +
                        "\n" +
                        "dishes.id,\n" +
                        "dishes.name as dish_name,\n" +
                        "dishes.iconLink as dish_iconLink,\n" +
                        "dishes.description,\n" +
                        "allergy_list.id as allergy_id,\n" +
                        "allergy_list.name as allergy_name,\n" +
                        "allergy_list.iconLink as allergy_iconLink\n" +
                        "\n" +
                        "FROM dishes\n" +
                        "\n" +
                        "JOIN dish_allergies\n" +
                        "ON dishes.id = dish_allergies.dish\n" +
                        "JOIN allergy_list\n" +
                        "ON dish_allergies.allergy = allergy_list.id\n" +
                        "\n" +
                        "WHERE\n" +
                        "dishes.isActive = true"
                val dishes = mutableMapOf<Int, Dish>()
                val query = db.executeQuery(stmt, arrayOf())
                if (query != null){
                    while (query.next()){
                        val id = query.getInt("id")

                        // Check if Dish has already been loaded.
                        // If so, just add some allergies
                        val dish = dishes[id]
                        if (dish != null){
                            println("Old dish found: ${query.getString("dish_name")}, Assigning ${query.getString("allergy_name")}")
                            dish.allergies.add(dish.allergies.size, AllergyWrapper(
                                query.getInt("allergy_id"),
                                query.getString("allergy_name"),
                                query.getString("allergy_iconLink")
                            ))
                        // Else it's a newly discovered dish.
                        } else {
                            println("New dish found: ${query.getString("dish_name")}")

                            dishes[id] = Dish(
                               id, true,
                               query.getString("dish_iconLink"),
                               query.getString("dish_name"),
                               mutableListOf(
                                   AllergyWrapper(
                                       query.getInt("allergy_id"),
                                       query.getString("allergy_name"),
                                       query.getString("allergy_iconLink")
                                   )
                               ),
                               query.getString("description")
                           )
                        }
                    }
                }

                // Convert into map
                for (d in dishes.values){
                    list.add(list.size, d)
                }

                // Wrap everything up and send it over
                body.data = GsonBuilder().create().toJson(list)
                body.type = 0

                println("Respond: $body")
                call.respond(body)

            } catch (ex: ContentTransformationException){
                println("Received invalid packet at '/getDishes'")
            }
        }

        post("/attemptReservation"){
            try {
                val body = call.receive<NetPack>()
                println("Received: $body")
                body.type = 1

                if (!authUserAsGuestOrAdmin(body.userToken)){
                    body.type = -1
                    println("Received unauthorized request at '/attempReservation'")
                    return@post
                }

                val wrapper = GsonBuilder().create().fromJson(body.data, ReservationWrapper::class.java)
                if (wrapper == null){
                    println("wrapper was null for reservationHolder")
                } else {

                    println("dbg: Wrapper valid")

                    // 'duration' of requested reservation
                    // Assign 35 minutes per person
                    // ToDo: dont scale linear...
                    val duration = wrapper.pplCurrent.coerceAtMost(wrapper.pplMax) * 35
                    val endpoint = Timestamp((wrapper.time) + (1000 * 60 * duration))

                    println("Duration: $duration")


                    // Double check current reservations
                    val reservationsRS = db.executeQuery("SELECT COUNT(*) as total FROM reservations " +
                            "WHERE layout = ? " +
                            "AND pos_x = ? " +
                            "AND pos_y = ? " +
                            "AND appointment_start > ? " +
                            "AND appointment_end < ?",

                        arrayOf(
                            wrapper.layout,
                            wrapper.x,
                            wrapper.y,
                            wrapper.time,
                            endpoint
                        ))
                    if (reservationsRS != null && reservationsRS.next() && reservationsRS.getInt("total") > 0){
                        println("There where already ${reservationsRS.getInt("total")} reservations at that point.")
                        body.type = -1
                    } else {

                        println("dbg: There are no concurrent reservatons!")


                        // Need to cache current creation time, because 'INSERT' statements only return the number of rows inserted, not an id
                        val timeRef = Timestamp(System.currentTimeMillis())
                        val update = db.executeUpdate("INSERT INTO reservations (layout, pos_x, pos_y, user, created, appointment_start, appointment_end, people) " +
                                "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                        arrayOf(
                            wrapper.layout,
                            wrapper.x,
                            wrapper.y,
                            getUserID(body.userToken),
                            timeRef,
                            Timestamp(wrapper.time),
                            endpoint,
                            wrapper.pplMax
                        ))

                        if (update == 1){

                            val reference = db.executeQuery("SELECT * FROM reservations WHERE created = ?", arrayOf(timeRef))
                            var reservationID = 0
                            if (reference != null && reference.next()){
                                reservationID = reference.getInt("id")
                            }

                            // Also add reservation dishes.
                            if (wrapper.dishes != null){
                                for (entry in wrapper.dishes!!.entries){

                                    val inserter = db.executeUpdate("INSERT INTO reservation_dishes (reservation, dish, amount) VALUES (?, ?, ?)",
                                    arrayOf(
                                        reservationID,
                                        entry.key,
                                        entry.value
                                    ))

                                    if (inserter == 0){
                                        println("Couldnt assign dish [${entry.key}], amount [${entry.value}] to reservation [$reservationID]")
                                    }
                                }
                            }
                            body.type = 0
                            body.data = (endpoint.time).toString()
                            call.respond(body)

                        } else {
                            println("Something went wrong?")
                            body.type = -1
                        }
                    }
                }


                println("Response: $body")
                call.respond(body)
            } catch (ex: ContentTransformationException){
                println("Received invalid packet at '/attemptReservation'")
            }
        }

        post("/deleteReservation"){
            // body.type == reservationID
            try {
                val body = call.receive<NetPack>()
                println("Received: $body")
                // ToDo: Limit deletion to x minutes before appointment!

                val userID = getUserID(body.userToken)
                val isAdmin = authUserAsAdmin(body.userToken)

                if (userID == -1){
                    body.type = -1
                    body.data = "Unauthorized request"
                    call.respond(body)
                    return@post
                }

                val selectQuery = db.executeQuery("SELECT * FROM reservations WHERE id = ?", arrayOf(body.type))
                if (selectQuery != null && selectQuery.next()){
                    if (selectQuery.getInt("user") == userID || isAdmin){

                        val deleteQry = db.executeUpdate("DELETE FROM reservations WHERE id = ?", arrayOf(body.type))
                        if (deleteQry == 1){
                            body.data = "Deletion successful - Removed reservation with id ${body.type}}"
                            body.type = 0
                        }

                    } else {
                        body.type = -1
                        body.data = "Unauthorized request"
                    }
                }

                call.respond(body)
                println("Response: $body")
            } catch (ex: Exception){
                println("Received invalid packet at '/deleteReservation'")
            }
        }

        post("/listReservations"){
            try {
                val body = call.receive<NetPack>()
                println("Received: $body")

                val userID = getUserID(body.userToken)
                val isAdmin = authUserAsAdmin(body.userToken)
                var loadAsAdmin = body.type

                if (userID == -1){
                    println("Unauthorized request at '/listReservations'")
                    body.type = -1
                    call.respond(body)
                    return@post
                }

                if (loadAsAdmin == 1 && !isAdmin){
                    loadAsAdmin = 0
                }


                var day: Timestamp? = null

                try {
                    day = GsonBuilder().create().fromJson(body.data, Timestamp::class.java)
                } catch (ex: Exception){
                    println("Invalid Timestamp provided: $ex")
                    body.type = -1
                    call.respond(body)
                    return@post
                }

                //day = Timestamp(day!!.time - (30 * 1000 * 60))

                val gregorian = GregorianCalendar()
                gregorian.time = day
                val anfangDesTages = day.time - (60 * 30)

                // Differentiate between admin and guest
                var query: ResultSet? = null
                if (loadAsAdmin == 0){
                    // --> GUEST
                    query = db.executeQuery("SELECT " +
                            "reservations.id, " +
                            "reservations.layout, " +
                            "reservations.appointment_start, " +
                            "reservations.appointment_end, " +
                            "reservations.people, " +
                            "users.username " +
                            "FROM " +
                            "reservations " +
                            "JOIN " +
                            "users " +
                            "ON  " +
                            "reservations.user = users.id " +
                            "WHERE " +
                            "reservations.appointment_start >= ? " +
                            "AND " +
                            "reservations.user = ? ORDER BY reservations.appointment_start ASC",
                        arrayOf(anfangDesTages, userID))
                } else {
                    // --> ADMIN
                    query = db.executeQuery("SELECT " +
                            "reservations.id, " +
                            "reservations.layout, " +
                            "reservations.appointment_start, " +
                            "reservations.appointment_end, " +
                            "reservations.people, " +
                            "users.username " +
                            "FROM " +
                            "reservations " +
                            "JOIN " +
                            "users " +
                            "ON  " +
                            "reservations.user = users.id " +
                            "ORDER BY reservations.appointment_start ASC",
                        arrayOf())

                }

                val list = mutableListOf<ReservationListEntry>()
                if (query != null){
                    while (query.next()){

                        if ((loadAsAdmin == 1 && isSameDay(Date(day.time), Date(query.getTimestamp("appointment_start").time))) || loadAsAdmin == 0){
                            println("Is on same day: ${query.getInt("id")}")

                            list.add(list.size, ReservationListEntry(
                                query.getInt("id"),
                                query.getInt("layout"),
                                query.getTimestamp("appointment_start").time,
                                query.getTimestamp("appointment_end").time,
                                query.getString("username"),
                                query.getInt("people")
                            ))
                        }
                    }
                }

                body.data = GsonBuilder().create().toJson(list)
                body.type = 0

                println("Response: $body")
                call.respond(body)
            } catch (ex: ContentTransformationException){
                ex.printStackTrace()
            }
        }

        post("/getAllLayouts"){

            try {
                val body = call.receive<NetPack>()
                println("Received: $body")
                if (!authUserAsAdmin(body.userToken)){
                    println("Received unauthorized request at '/getAllLayouts'")
                    body.type = -1
                    body.data = ""
                    call.respond(body)
                    return@post
                }

                val rs = db.executeQuery("SELECT * FROM layouts ORDER BY validFrom DESC", arrayOf())
                if (rs == null){
                    body.type = 1
                    body.data = "Database returns null"
                    call.respond(body)
                    return@post
                }

                val list: MutableList<LayoutWrapper> = mutableListOf()
                while (rs.next()){

                    val wrapper = LayoutWrapper(
                        rs.getInt("id"),
                        rs.getInt("size_x"),
                        rs.getInt("size_y"),
                        rs.getString("name"),
                        rs.getTimestamp("created").time,
                        rs.getTimestamp("validFrom").time,
                        rs.getBoolean("active"),
                        mutableListOf())
                    wrapper.fillFromString(rs.getString("data"))

                    list.add(list.size, wrapper)
                }

                body.data = GsonBuilder().create().toJson(list)
                body.type = 0
                call.respond(body)
                println("Response: $body")

            } catch (ex: ContentTransformationException){
                println("Received invalid packet at '/getAllLayouts'")
            }
        }

        post("/getReservationDetails"){
            try {
                val body = call.receive<NetPack>()

                println("Received: $body")

                val userID = getUserID(body.userToken)
                val isAdmin = authUserAsAdmin(body.userToken)

                if (userID == -1){
                    body.type = -1
                    call.respond(body)
                    return@post
                }

                val query = db.executeQuery("SELECT * FROM reservations WHERE id = ?", arrayOf(body.type))

                if (query != null && query.next()){
                    val wrapper = ReservationWrapper(
                        query.getInt("layout"),
                        query.getInt("pos_x"),
                        query.getInt("pos_y"),
                        query.getTimestamp("appointment_start").time,
                        query.getInt("people"),
                        query.getInt("people"),
                        mutableMapOf()
                    )


                    // Check if this reservation belongs to the querying user
                    val resID = query.getInt("user")
                    if (resID != userID && !isAdmin){
                        body.type = -1
                        body.data = "This doesnt belong to you!"
                        call.respond(body)
                        return@post
                    }

                    val dishQuery = db.executeQuery("SELECT * FROM reservation_dishes WHERE reservation = ?", arrayOf(query.getInt("id")))
                    if (dishQuery != null){
                        while (dishQuery.next()){
                            wrapper.dishes!![dishQuery.getInt("dish")] = dishQuery.getInt("amount")
                        }
                    }

                    body.data = GsonBuilder().create().toJson(wrapper)
                } else {
                    body.data = ""
                    body.type = 0
                }

                call.respond(body)
                println("Response: $body")
            } catch (ex: Exception){
                ex.printStackTrace()
            }
        }



    }
}
