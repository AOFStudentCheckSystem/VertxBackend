package cn.codetector.guardianCheck.server.data.user

import cn.codetector.guardianCheck.server.data.permission.Permission
import cn.codetector.guardianCheck.server.data.permission.PermissionManager
import cn.codetector.guardianCheck.server.data.permission.Role
import cn.codetector.util.Validator.SHA

class User(val username: String, val passwordHash: String, val role: Role) {
    fun hasPermission(permission: Permission): Boolean {
        return role.hasPermission(permission)
    }

    fun hasPermission(permission: String): Boolean {
        return role.hasPermission(PermissionManager.getPermissionByName(permission))
    }

    fun authenticate(password: String): Boolean {
        return SHA.getSHA256String(password) == (this.passwordHash)
    }
}