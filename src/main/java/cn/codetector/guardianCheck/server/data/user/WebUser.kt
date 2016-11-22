package cn.codetector.guardianCheck.server.data.user

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider

/**
 * Created by codetector on 21/11/2016.
 */
class WebUser (val user:User): io.vertx.ext.auth.User{
    var lastActive = System.currentTimeMillis()
        private set
    override fun isAuthorised(authority: String?, resultHandler: Handler<AsyncResult<Boolean>>?): io.vertx.ext.auth.User {
        resultHandler!!.handle(Future.succeededFuture(user.hasPermission(authority!!)))
        return this
    }

    fun renewTime(){
        this.lastActive = System.currentTimeMillis()
    }

    override fun clearCache(): io.vertx.ext.auth.User {
        return this
    }

    override fun setAuthProvider(authProvider: AuthProvider?) {

    }

    override fun principal(): JsonObject {
        return JsonObject().put("username",this.user.username)
    }

    fun lastActiveInRange(timeRange: Long):Boolean{
        return (Math.abs(System.currentTimeMillis() - lastActive) < timeRange)
    }
}