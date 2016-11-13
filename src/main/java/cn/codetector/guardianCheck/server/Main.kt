 package cn.codetector.guardianCheck.server

import cn.codetector.guardianCheck.server.database.SharedDBManager
import cn.codetector.guardianCheck.server.webService.WebService
import io.vertx.core.Vertx
import io.vertx.ext.jdbc.JDBCClient

fun main(args:Array<String>){
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory")
    val sharedVertx:Vertx = Vertx.vertx()
    val sharedJDBCClient:JDBCClient = JDBCClient.createShared(sharedVertx,SharedDBManager.dbConfigObject)
    try {
        WebService.initService(sharedVertx,sharedJDBCClient)
    }catch (t : Throwable){
        t.printStackTrace();
    }
}