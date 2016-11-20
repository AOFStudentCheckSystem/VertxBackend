package cn.codetector.guardianCheck.server.permission

import io.vertx.core.json.JsonArray

class Role (val id:Int, val name:String){
    val permissions:PermissionMap = PermissionMap()

    fun addPermission(permission: Permission){
        permissions.add(permission)
    }

    fun loadPermissionFromJSONArray(jsonArray:JsonArray){
        
    }

}