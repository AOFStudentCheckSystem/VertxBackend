package cn.codetector.guardianCheck.server.permission

import io.vertx.ext.jdbc.JDBCClient

/**
 * Created by codetector on 19/11/2016.
 */
object PermissionManager {
    private var isInitialized = false
    private var dbClient:JDBCClient? = null

    fun isInitialized():Boolean{
        return this.isInitialized
    }
    fun setDBClient(dbClient:JDBCClient){
        this.dbClient = dbClient
    }
}