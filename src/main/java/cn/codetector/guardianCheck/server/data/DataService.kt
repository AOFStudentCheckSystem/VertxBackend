/*
 * Copyright (c) 2016. Codetector (Yaotian Feng)
 */

package cn.codetector.guardianCheck.server.data

import cn.codetector.guardianCheck.server.Main
import cn.codetector.guardianCheck.server.data.permission.PermissionManager
import cn.codetector.guardianCheck.server.data.students.StudentManager
import cn.codetector.guardianCheck.server.data.user.UserHash
import cn.codetector.guardianCheck.server.data.user.UserManager
import io.vertx.core.logging.LoggerFactory
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by Codetector on 20/11/2016.
 */
object DataService {
    val logger = LoggerFactory.getLogger(this.javaClass)
    val executors: ExecutorService = Executors.newSingleThreadExecutor()

    fun start() {
        logger.info("Starting DataService")
        PermissionManager.setDBClient(Main.sharedJDBCClient)
        UserManager.setDBClient(Main.sharedJDBCClient)
        StudentManager.setDBClient(Main.sharedJDBCClient)
        executors.submit(DataServiceTicker(5000, {
            PermissionManager.tick()
            UserManager.tick()
            StudentManager.tick()
            UserHash.tick()
        }))
        load()
    }

    fun save() {
        save {}
    }

    fun save(action: () -> Unit) {
        PermissionManager.saveToDatabase {
            UserManager.saveToDatabase {
                StudentManager.saveToDatabase {
                    action.invoke()
                }
            }
        }
    }

    fun terminate() {
        executors.awaitTermination(3, TimeUnit.SECONDS)
        executors.shutdown()
    }

    fun isTerminated(): Boolean {
        return executors.isTerminated
    }

    fun reload() {
        load()
    }

    fun load() {
        PermissionManager.loadFromDatabase {
            UserManager.loadFromDatabase {
                StudentManager.loadFromDatabase {
                    UserHash.loadCache()
                    logger.info("Data Service Loaded")
                }
            }
        }
    }
}