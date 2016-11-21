package cn.codetector.guardianCheck.server.data.user

import cn.codetector.guardianCheck.server.data.AbstractDataService
import cn.codetector.guardianCheck.server.data.permission.PermissionManager
import io.vertx.ext.jdbc.JDBCClient

object UserManager : AbstractDataService(){
    override fun initialize() {

    }

    override fun saveToDatabase(action: () -> Unit) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun loadFromDatabase(action: () -> Unit) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

}