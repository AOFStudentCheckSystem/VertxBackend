package cn.codetector.guardianCheck.server.data.user

import io.vertx.core.logging.LoggerFactory
import java.util.*

object UserHash {
    val DEFAULT_TIMEOUT:Long = 1000 //* 60 * 30
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val allUsers: MutableMap<String, WebUser> = HashMap()

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