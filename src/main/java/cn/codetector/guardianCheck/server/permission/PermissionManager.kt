package cn.codetector.guardianCheck.server.permission

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.ext.jdbc.JDBCClient

/**
 * Created by codetector on 19/11/2016.
 */
object PermissionManager {
    private var dbClient:JDBCClient? = null
    private val serverPermissions = PermissionMap()

    fun isInitialized():Boolean{
        return this.dbClient != null
    }
    fun setDBClient(dbClient:JDBCClient){
        this.dbClient = dbClient
    }

    fun loadPermissionsFromServer(){
        dbClient!!.getConnection { conn ->
            if (conn.succeeded()){
                conn.result().query("SELECT * FROM `dummypermission`", {
                    result ->
                    if (result.succeeded()){
                        serverPermissions.clear()
                        result.result().rows.forEach {
                            row ->
                            serverPermissions.addPermission(Permission(row.getInteger("id"),row.getString("name"),row.getString("description")))
                        }
                    }
                })
            }
        }
    }
}