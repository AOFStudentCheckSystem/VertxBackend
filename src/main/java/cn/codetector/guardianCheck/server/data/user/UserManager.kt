package cn.codetector.guardianCheck.server.data.user

import cn.codetector.guardianCheck.server.data.AbstractDataService
import cn.codetector.guardianCheck.server.data.permission.PermissionManager
import io.vertx.core.json.JsonArray
import io.vertx.core.logging.LoggerFactory
import java.util.*

object UserManager : AbstractDataService() {
    val allUsers: MutableMap<String, User> = HashMap()
    val logger = LoggerFactory.getLogger(this.javaClass)

    override fun initialize() {
        logger.info("User Manager Initialized")
    }

    override fun saveToDatabase(action: () -> Unit) {
        dbClient!!.getConnection { conn ->
            logger.trace("Saving users to database...")
            val users: MutableList<JsonArray> = ArrayList()
            this.allUsers.values.forEach { user ->
                users.add(JsonArray().add(user.username).add(user.passwordHash).add(user.role.name))
            }
            if (conn.succeeded()) {
                conn.result().batchWithParams("INSERT INTO `dummyauth` (`username`,`password`,`role`) VALUES (?,?,?) ON DUPLICATE KEY UPDATE `password` = VALUES(`password`), `role` = VALUES(`role`)", users, {
                    result ->
                    if (result.succeeded()) {
                        var success = 0
                        var fail = 0
                        result.result().forEach {
                            if (it == 1) success++ else fail++
                        }
                        logger.trace("User save complete, Succeed:$success, Failed:$fail")
                    }
                    action.invoke()
                })
            } else {
                logger.warn("Failed to initialize database connection", conn.cause())
                action.invoke()
            }
        }
    }

    override fun loadFromDatabase(action: () -> Unit) {
        assert(isInitialized())
        dbClient!!.getConnection { conn ->
            logger.trace("Loading Users from Database...")
            if (conn.succeeded()) {
                conn.result().query("SELECT * FROM `dummyauth`", { result ->
                    allUsers.clear()
                    result.result().rows.forEach { row ->
                        val user = User(row.getString("username"), row.getString("password"), PermissionManager.getRoleByName(row.getString("role")))
                        allUsers.put(user.username, user)
                    }
                    val userCount = allUsers.size
                    logger.trace("User load complete, $userCount user(s) loaded")
                    action.invoke()
                })
            } else {
                logger.warn("Failed to initialize database connection", conn.cause())
                action.invoke()
            }
        }
    }

    fun hasUser(username: String): Boolean {
        return allUsers.containsKey(username)
    }

    fun getUserByUsername(username: String): User {
        try {
            return allUsers.get(username)!!
        } catch (e: Throwable) {
            throw IllegalArgumentException("requested User does not exist. Please check use hasUser(username) before requesting")
        }
    }

}