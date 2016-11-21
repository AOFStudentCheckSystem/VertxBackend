package cn.codetector.guardianCheck.server.data.database

import io.vertx.core.logging.LoggerFactory
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

class ConnectionManager(val url:String, val username:String, val password:String) {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    private var connection: Connection? = null

    init{
        try {
            Class.forName("com.mysql.jdbc.Driver")
        }catch (e: ClassNotFoundException){
            logger.error("Failed to load MySQL JDBC Driver", e)
        }
    }

    fun getConnection():Connection{
        if (connection != null && connection!!.isValid(1)){
            return connection!!
        }else{
            this.createConnection()
            return connection!!
        }
    }

    private fun createConnection() {
        try {
            this.connection = DriverManager.getConnection(url, username, password)
        } catch (e:SQLException){
            logger.error("Failed to create SQL Connection",e)
        }
    }
}