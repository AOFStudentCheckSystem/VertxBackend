package cn.codetector.guardianCheck.server.data

import io.vertx.ext.jdbc.JDBCClient

/**
 * Created by Codetector on 21/11/2016.
 */
abstract class AbstractDataService {
    protected var dbClient: JDBCClient? = null

    fun isInitialized(): Boolean {
        return this.dbClient != null
    }

    fun setDBClient(dbClient: JDBCClient) {
        this.dbClient = dbClient
        initialize()
    }

    abstract fun initialize()

    abstract fun saveToDatabase(action: () -> Unit)
    fun saveToDatabase() {
        saveToDatabase {}
    }

    abstract fun loadFromDatabase(action: () -> Unit)
    fun loadFromDatabase() {
        loadFromDatabase {}
    }
}