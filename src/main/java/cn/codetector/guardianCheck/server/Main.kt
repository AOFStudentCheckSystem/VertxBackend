package cn.codetector.guardianCheck.server

import cn.codetector.guardianCheck.server.database.SharedDBManager
import io.vertx.core.Vertx
import io.vertx.ext.jdbc.JDBCClient

fun main(args:Array<String>){
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory")
    val sharedVertx:Vertx = Vertx.vertx()
    var sharedJDBCClient:JDBCClient = JDBCClient.createShared(sharedVertx,SharedDBManager.dbConfigObject)
}