package cn.codetector.guardianCheck.server.data.user

import cn.codetector.util.FileUtil.FileReader
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.util.*

object UserHash {
    val DEFAULT_TIMEOUT: Long = 1000 * 60 * 30
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val allUsers: MutableMap<String, WebUser> = HashMap()

    private val targetCacheFile = File("./session.cache")

    init {
        logger.trace("Initializing UserHash database")
        if (targetCacheFile.exists() && targetCacheFile.isFile){
            val dataString = FileReader.readFile(targetCacheFile)
            if (dataString.isNotBlank()) {
                val cachedData = JsonObject(dataString).getJsonArray("cache")
                cachedData.forEach { item->
                    allUsers.put((item as JsonObject).getString("key"),WebUser(UserManager.getUserByUsername(item.getString("user")),item.getLong("lastActive")))
                }
                logger.trace("All (${allUsers.size}) user cache loaded")
                removeTimedOutUsers(DEFAULT_TIMEOUT)
            }
        }
    }

    fun save(){
        logger.trace("Saving login Cache...")
        removeTimedOutUsers(DEFAULT_TIMEOUT)
        val writer = PrintWriter(FileWriter(targetCacheFile))
        val dataArray = JsonArray()
        allUsers.forEach { entry ->
            dataArray.add(JsonObject().put("key",entry.key).put("user",entry.value.user.username).put("lastActive",entry.value.lastActive))
        }
        writer.print(JsonObject().put("cache",dataArray).toString())
        writer.close()
        logger.trace("Login Cache saved!")
    }

    internal fun totalUserCache(): Int {
        return allUsers.size
    }

    fun createWebUser(user: User): String {
        var uniqueId = UUID.randomUUID().toString()
        while (allUsers.containsKey(uniqueId)) {
            uniqueId = UUID.randomUUID().toString()
        }
        allUsers.put(uniqueId, WebUser(user))
        return uniqueId
    }

    fun isAuthKeyValid(key: String): Boolean {
        return allUsers.containsKey(key)
    }

    fun getUserByAuthKey(key: String): WebUser {
        return allUsers.get(key)!!
    }

    fun removeTimedOutUsers(valveValue: Long) {
        logger.trace("Removing timed out user(s)")
        var count = 0
        val it = allUsers.iterator()
        while (it.hasNext()) {
            if (!it.next().value.lastActiveInRange(valveValue)) {
                it.remove()
                count++
            }
        }
        logger.trace("$count timed out user(s) removed")
    }
}