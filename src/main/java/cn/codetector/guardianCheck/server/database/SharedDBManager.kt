package cn.codetector.guardianCheck.server.database

import cn.codetector.util.Configuration.Configuration
import cn.codetector.util.Configuration.ConfigurationManager
import io.vertx.core.json.JsonObject

/**
 * Created by Codetector on 2016/7/14.
 */
object SharedDBManager {
    private val dbConfig = ConfigurationManager.getConfiguration("mysql.json")
    var dbPrefix: String? = null
        private set

    init {
        dbPrefix = dbConfig.getStringValue("db_prefix", "gm_")
    }

    val dbConfigObject: JsonObject
        get() {
            val jsonObject = JsonObject()

            jsonObject.put("driver_class", dbConfig.getStringValue("driver_class", "com.mysql.cj.jdbc.Driver"))
            jsonObject.put("user", dbConfig.getStringValue("user", "root"))
            jsonObject.put("password", dbConfig.getStringValue("password", ""))
            jsonObject.put("max_pool_size", dbConfig.getIntergerValue("max_pool_size", 15))
            jsonObject.put("initial_pool_size", dbConfig.getIntergerValue("initial_pool_size", 3))
            jsonObject.put("url", dbConfig.getStringValue("url", "jdbc:mysql://localhost:3306/") + dbConfig.getStringValue("db_name", "sentora_postfix") + "?useSSL=" + dbConfig.getStringValue("useSSL", "false") + "&characterEncoding=" + dbConfig.getStringValue("charSet", "utf-8"))
            return jsonObject
        }
}
