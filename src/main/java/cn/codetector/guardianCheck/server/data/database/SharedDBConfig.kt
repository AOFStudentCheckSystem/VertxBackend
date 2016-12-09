/*
 * Copyright (c) 2016. Codetector (Yaotian Feng)
 */

package cn.codetector.guardianCheck.server.data.database

import cn.codetector.util.Configuration.ConfigurationManager
import io.vertx.core.json.JsonObject

/**
 * Created by Codetector on 2016/7/14.
 */
object SharedDBConfig {
    private val dbConfig = ConfigurationManager.getConfiguration("mysql.json")

    val driver_class = dbConfig.getStringValue("driver_class", "com.mysql.cj.jdbc.Driver")
    val user = dbConfig.getStringValue("user", "root")
    val password = dbConfig.getStringValue("password", "")
    val max_pool_size = dbConfig.getIntegerValue("max_pool_size", 15)
    val initial_pool_size = dbConfig.getIntegerValue("initial_pool_size", 3)
    val db_url = dbConfig.getStringValue("url", "jdbc:mysql://localhost:3306/")
    val db_name = dbConfig.getStringValue("db_name", "sentora_postfix")
    val db_ssl = dbConfig.getStringValue("useSSL", "false")
    val db_charset = dbConfig.getStringValue("charSet", "utf-8")
    val db_max_idle_time = dbConfig.getIntegerValue("db_max_idle_time", 30)

    var dbPrefix: String? = null
        private set

    init {
        dbPrefix = dbConfig.getStringValue("db_prefix", "dummy")
    }

    fun getDBConnectionURLWithSettings(): String {
        return "$db_url$db_name?useSSL=$db_ssl&characterEncoding=$db_charset"
    }

    fun getVertXJDBCConfigObject(): JsonObject {
        val jsonObject = JsonObject()
        jsonObject.put("driver_class", driver_class)
        jsonObject.put("user", user)
        jsonObject.put("password", password)
        jsonObject.put("max_pool_size", max_pool_size)
        jsonObject.put("initial_pool_size", initial_pool_size)
        jsonObject.put("url", getDBConnectionURLWithSettings())
        jsonObject.put("max_idle_time", db_max_idle_time)
        return jsonObject
    }

}
