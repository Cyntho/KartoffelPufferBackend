package org.cyntho.fh.database

import java.sql.Connection
import java.sql.Date
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Timestamp

class DbConnector(private val host: String,
                  private val port: Int,
                  private val user: String,
                  private val pass: String,
                  private val database: String) {

    private lateinit var connection: Connection


    init {
        try {
            Class.forName("com.mysql.jdbc.Driver")
            connection = DriverManager.getConnection(getConnectionString(), user, pass)
            println("Database connection established.")
        } catch (ex: ClassNotFoundException){
            println("Could not find driver: ${ex.message}")
        }
    }

    private fun getConnectionString(): String {
        return "jdbc:mysql://$host:$port/$database?user=$user&pass=$pass"
    }

    public fun executeQuery(qry: String, args: Array<Any>): ResultSet? {
        try {

            val stmt: PreparedStatement = prepare(qry, args)

            return stmt.executeQuery()
        } catch (num: java.lang.NumberFormatException){
            println("DbConnector.executeQuery() --> Invalid number format: ${num.cause}")
        } catch (any: java.lang.Exception){
            any.printStackTrace()
        }
        return null
    }

    public fun executeUpdate(qry: String, args: Array<Any>): Int {
        try {
            val stmt: PreparedStatement = prepare(qry, args)

            return stmt.executeUpdate()
        } catch (any: java.lang.Exception){
            any.printStackTrace()
        }
        return -1;
    }

    private fun prepare(qry: String, args: Array<Any>): PreparedStatement {
        val stmt: PreparedStatement = connection.prepareStatement(qry)

        var i = 1
        for (a: Any in args){

            when (a) {
                is Int -> {
                    stmt.setInt(i, a)
                }
                is String -> {
                    stmt.setString(i, a)
                }
                is Boolean -> {
                    stmt.setBoolean(i, a)
                }
                is Long -> {
                    stmt.setLong(i, a)
                }
                is Float -> {
                    stmt.setFloat(i, a)
                }
                is Double -> {
                    stmt.setDouble(i, a)
                }
                is Date -> {
                    stmt.setDate(i, a)
                }
                is Timestamp -> {
                    stmt.setTimestamp(i, a)
                }
                else -> {
                    println("DbConnector.executeQuery() --> Undefined object type for: $a")
                }
            }
            i += 1
        }

        return stmt
    }
}