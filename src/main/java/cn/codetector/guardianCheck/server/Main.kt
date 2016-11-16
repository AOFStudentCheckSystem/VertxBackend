package cn.codetector.guardianCheck.server

import cn.codetector.guardianCheck.server.database.SharedDBManager
import cn.codetector.guardianCheck.server.webService.WebService
import cn.codetector.util.Configuration.ConfigurationManager
import io.vertx.core.Vertx
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.core.VertxOptions

fun main(args: Array<String>) {
    val globalConfig = ConfigurationManager.getConfiguration("mainConfig.json")
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory")
    val sharedVertx: Vertx = Vertx.vertx(VertxOptions().setWorkerPoolSize(globalConfig.getIntergerValue("workerPoolSize",32)))
    val sharedJDBCClient: JDBCClient = JDBCClient.createShared(sharedVertx, SharedDBManager.dbConfigObject)
    try {
        WebService.initService(sharedVertx, sharedJDBCClient)
    } catch (t: Throwable) {
        t.printStackTrace();
    }
}