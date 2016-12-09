/*
 * Copyright (c) 2016. Codetector (Yaotian Feng)
 */

package cn.codetector.guardianCheck.server.data.user

import io.vertx.core.AsyncResult
import io.vertx.core.Future
import io.vertx.core.Handler
import io.vertx.core.json.JsonObject
import io.vertx.ext.auth.AuthProvider
import io.vertx.ext.auth.User

/**
 * Created by codetector on 21/11/2016.
 */
class IDAuthProvider : AuthProvider {

    override fun authenticate(authInfo: JsonObject?, resultHandler: Handler<AsyncResult<User>>?) {
        if (authInfo!!.containsKey("auth")) {
            if (UserHash.isAuthKeyValid(authInfo.getString("auth"))) {
                resultHandler!!.handle(Future.succeededFuture(UserHash.getUserByAuthKey(authInfo.getString("auth"))))
            } else {
                resultHandler!!.handle(Future.failedFuture("Invalid Token"))
            }
        } else {
            resultHandler!!.handle(Future.failedFuture("Malformed authInfo"))
        }
    }
}