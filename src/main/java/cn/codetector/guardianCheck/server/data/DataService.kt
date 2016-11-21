package cn.codetector.guardianCheck.server.data

import cn.codetector.guardianCheck.server.Main
import cn.codetector.guardianCheck.server.data.permission.Permission
import cn.codetector.guardianCheck.server.data.permission.PermissionManager
import io.vertx.core.logging.LoggerFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by Codetector on 20/11/2016.
 */
object DataService {
    val logger = LoggerFactory.getLogger(this.javaClass)
    val executors:ExecutorService = Executors.newSingleThreadExecutor()

    fun start(){
        logger.info("Starting DataService")
        PermissionManager.setDBClient(Main.sharedJDBCClient)
        load()
    }

    fun save(){
        save {}
    }

    fun save(action: () -> Unit){
        PermissionManager.saveToDatabase {
            action.invoke()
        }
    }

    fun terminate(){
        executors.awaitTermination(3,TimeUnit.SECONDS)
        executors.shutdown()
    }

    fun isTerminated():Boolean{
        return executors.isTerminated
    }

    fun reload() {
        load()
    }

    fun load(){
        PermissionManager.loadFromDatabase{
            logger.info("Data Service Loaded")
        }
    }
}