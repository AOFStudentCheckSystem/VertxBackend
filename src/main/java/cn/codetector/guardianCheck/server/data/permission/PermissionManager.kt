package cn.codetector.guardianCheck.server.data.permission

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.jdbc.JDBCClient
import java.util.*

/**
 * Created by codetector on 19/11/2016.
 */
object PermissionManager {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    private var dbClient: JDBCClient? = null
    private val serverPermissions = PermissionMap()
    private val serverRoles = HashMap<String, Role>()

    fun isInitialized(): Boolean {
        return this.dbClient != null
    }

    fun setDBClient(dbClient: JDBCClient) {
        this.dbClient = dbClient
        logger.info("Permission Manager Initialized")
    }

    fun getPermissionByName(name: String): Permission {
        if (serverPermissions.permissions.contains(name)) {
            return serverPermissions.permissions.get(name)!!
        } else {
            throw IllegalArgumentException("No Permission named '$name' Found")
        }
    }

    fun getRoleByName(name: String): Role {
        if (serverRoles.contains(name)) {
            return serverRoles.get(name)!!
        } else {
            throw IllegalArgumentException("No Role named '$name' Found")
        }
    }

    private fun getPermissionWithName(name: String): Permission {
        if (serverPermissions.permissions.contains(name)) {
            return serverPermissions.permissions.get(name)!!
        } else {
            val perm = Permission(name, "Ability to $name")
            logger.trace("Permission created on use : $perm")
            serverPermissions.addPermission(perm)
            return perm
        }
    }

    fun savePermissionTable(action: () -> Unit) {
        assert(isInitialized())
        logger.trace("Saving Permission Table...")
        dbClient!!.getConnection { conn ->
            if (conn.succeeded()) {
                val params = ArrayList<JsonArray>()
                serverPermissions.permissions.values.forEach { perm ->
                    params.add(JsonArray().add(perm.name).add(perm.description))
                }
                conn.result().batchWithParams("REPLACE INTO `dummypermission` (`name`, `description`) VALUES (? ,? )", params, {
                    handler ->
                    if (handler.succeeded()) {
                        var success = 0
                        var fail = 0
                        handler.result().forEachIndexed { i, result ->
                            if (result == 1) success++ else fail++
                        }
                        logger.trace("Permission save complete, Success: $success, Fail: $fail")
                    }
                    action.invoke()
                })
            } else {
                logger.error("Failed to save permission table", conn.cause())
                action.invoke()
            }
        }
    }

    fun saveRolesTable(action: () -> Unit) {
        assert(isInitialized())
        logger.trace("Saving Roles Table...")
        dbClient!!.getConnection { conn ->
            if (conn.succeeded()) {
                val roles = ArrayList<JsonArray>()
                serverRoles.values.forEach { role ->
                    roles.add(JsonArray().add(role.name).add(role.getPermissionJson().toString()))
                }
                conn.result().batchWithParams("INSERT IGNORE INTO `dummyroles` (`name`, `permissions`) VALUES (? ,? )", roles, {
                    handler ->
                    if (handler.succeeded()) {
                        var new = 0
                        var unChange = 0
                        handler.result().forEachIndexed { i, result ->
                            if (result == 1) new++ else unChange++
                        }
                        logger.trace("Role save complete, New: $new, Unchanged: $unChange")
                    }else{
                        logger.error("Failed to save Role table", handler.cause())
                    }
                    action.invoke()
                })
            } else {
                logger.error("Failed to save Role table", conn.cause())
                action.invoke()
            }
        }
    }

    fun saveToDatabase(action: () -> Unit) {
        savePermissionTable {
            saveRolesTable {
                action.invoke()
            }
        }
    }

    fun loadFromDatabase(){
        loadFromDatabase{}
    }

    fun loadFromDatabase(action: () -> Unit) {
        this.loadPermissionsFromDatabase {
            this.loadRolesFromDatabase {
                action.invoke()
            }
        }
    }

    fun loadPermissionsFromDatabase(action: () -> Unit) {
        assert(isInitialized())
        dbClient!!.getConnection { conn ->
            if (conn.succeeded()) {
                logger.trace("Loading permissions from database...")
                conn.result().query("SELECT * FROM `dummypermission`", {
                    result ->
                    if (result.succeeded()) {
                        serverPermissions.clear()
                        result.result().rows.forEach {
                            row ->
                            val perm = Permission(row.getString("name"), row.getString("description"))
                            serverPermissions.addPermission(perm)
                        }
                        val permCount = serverPermissions.allPermissions().size
                        logger.trace("All ($permCount) Permission Loaded")
                    }
                    conn.result().close()
                    action.invoke()
                })
            }
        }
    }

    fun loadRolesFromDatabase(action: () -> Unit) {
        logger.trace("Loading serverRoles from database...")
        dbClient!!.getConnection { conn ->
            if (conn.succeeded()) {
                conn.result().query("SELECT * FROM `dummyroles`", {
                    query ->
                    if (query.succeeded()) {
                        query.result().rows.forEach { roleR ->
                            var role = Role(roleR.getString("name"))
                            JsonObject(roleR.getString("permissions")).getJsonArray("permissions").forEach { permission ->
                                if (permission is String){
                                    role.addPermission(PermissionManager.getPermissionWithName(permission))
                                }
                            }
                            serverRoles.put(role.name,role)
                        }
                    }
                    val rolesCount = serverRoles.size
                    logger.trace("All ($rolesCount) Roles Loaded")
                    action.invoke()
                })
            } else {

            }
        }
    }
}