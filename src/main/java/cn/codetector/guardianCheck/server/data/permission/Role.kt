package cn.codetector.guardianCheck.server.data.permission

import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject

class Role (val id:Int, val name:String){
    val permissions: PermissionMap = PermissionMap()

    constructor(jsonObject: JsonObject):this(jsonObject.getInteger("id"),jsonObject.getString("magic")){

    }

    fun addPermission(permission: String){

    }

    fun allPermissions():List<Permission>{
        return permissions.allPermissions()
    }
}