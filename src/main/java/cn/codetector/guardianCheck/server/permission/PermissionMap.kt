package cn.codetector.guardianCheck.server.permission

import io.vertx.core.json.JsonArray
import java.util.*

class PermissionMap {
    val permissions:HashMap<String, Permission> = HashMap()

    fun addPermission(permission: Permission){
        permissions.put(permission.name,permission)
    }

    fun allPermissions():List<Permission>{
        return ArrayList(permissions.values)
    }

    override fun toString(): String {
        val rootArray = JsonArray()
        permissions.values.forEach { permission ->
            rootArray.add(permission.name)
        }
        return rootArray.toString()
    }

    fun clear() {
        this.permissions.clear()
    }
}