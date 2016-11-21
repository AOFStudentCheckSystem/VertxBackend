package cn.codetector.guardianCheck.server

import cn.codetector.guardianCheck.server.data.database.SharedDBConfig
import cn.codetector.guardianCheck.server.data.permission.PermissionManager
import cn.codetector.guardianCheck.server.webService.WebService
import cn.codetector.util.Configuration.ConfigurationManager
import io.vertx.core.Vertx
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.core.VertxOptions
import org.apache.logging.log4j.LogManager

fun main(args: Array<String>) {
    val logger = LogManager.getLogger("Server Root")
    val globalConfig = ConfigurationManager.getConfiguration("mainConfig.json")
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory")
    val sharedVertx: Vertx = Vertx.vertx(VertxOptions().setWorkerPoolSize(globalConfig.getIntergerValue("workerPoolSize",32)))
    val sharedJDBCClient: JDBCClient = JDBCClient.createShared(sharedVertx, SharedDBConfig.getVertXJDBCConfigObject())
    try {
        PermissionManager.setDBClient(sharedJDBCClient)//Init Permission System Before anything else
        WebService.initService(sharedVertx, sharedJDBCClient) //Init Web API Services
    } catch (t: Throwable) {
        logger.error(t)
    }
}