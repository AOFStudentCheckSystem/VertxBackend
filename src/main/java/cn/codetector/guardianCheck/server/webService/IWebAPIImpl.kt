package cn.codetector.guardianCheck.server.webService

import io.vertx.core.Vertx
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.web.Router

/**
 * Created by Codetector on 2016/11/12.
 */
interface IWebAPIImpl {
    fun initAPI(router:Router, sharedVertx:Vertx, dbClient:JDBCClient)
}