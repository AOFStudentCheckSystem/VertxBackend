/*
 * Copyright (c) 2016. Codetector (Yaotian Feng)
 */

package cn.codetector.guardianCheck.server

import cn.codetector.guardianCheck.server.console.consoleManager.ConsoleManager
import cn.codetector.guardianCheck.server.data.DataService
import cn.codetector.guardianCheck.server.data.database.SharedDBConfig
import cn.codetector.guardianCheck.server.data.user.UserHash
import cn.codetector.guardianCheck.server.webService.WebService
import cn.codetector.util.Configuration.ConfigurationManager
import io.vertx.core.Vertx
import io.vertx.core.VertxOptions
import io.vertx.ext.jdbc.JDBCClient
import org.apache.logging.log4j.LogManager
import java.util.*

object Main {
    val rootLogger = LogManager.getLogger("Server Root")
    val globalConfig = ConfigurationManager.getConfiguration("mainConfig.json")
    val sharedVertx: Vertx = Vertx.vertx(VertxOptions().setWorkerPoolSize(globalConfig.getIntegerValue("workerPoolSize", 32)))
    val sharedJDBCClient: JDBCClient = JDBCClient.createShared(sharedVertx, SharedDBConfig.getVertXJDBCConfigObject())

    init {
        rootLogger.info("Starting Server...")
    }

    fun initService() {
        try {
            DataService.start()
            WebService.initService(Main.sharedVertx, Main.sharedJDBCClient) //Init Web API Services
        } catch (t: Throwable) {
            Main.rootLogger.error(t)
        }
        ConsoleManager.monitorStream("ConsoleIn", System.`in`)
    }

    fun save() {
        UserHash.save()
        DataService.save()
    }

    fun stopService() {
        Main.rootLogger.info("Shutting down Server")
        ConsoleManager.stop()
        WebService.shutdown()
        UserHash.save()
        DataService.terminate()
        DataService.save {
            //TODO move database shutdown into Dataservice
            Main.rootLogger.info("Disconnecting from Database")
            Main.sharedJDBCClient.close()
            Main.rootLogger.info("All Database connection shutdown")
            Main.sharedVertx.close({ res ->
                if (res.succeeded()) {
                    Main.rootLogger.info("Vert.X Shutdown")
                }
            })
        }

    }
}

fun main(args: Array<String>) {
    System.setProperty("vertx.logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory")
    System.setProperty("logger-delegate-factory-class-name", "io.vertx.core.logging.Log4j2LogDelegateFactory")
    TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    Main.initService()
}