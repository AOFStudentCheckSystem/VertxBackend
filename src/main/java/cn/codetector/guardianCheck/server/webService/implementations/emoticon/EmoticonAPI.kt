package cn.codetector.guardianCheck.server.webService.implementations.emoticon

import cn.codetector.guardianCheck.server.webService.IWebAPIImpl
import cn.codetector.guardianCheck.server.webService.WebAPIImpl
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.web.Router

/**
 * Created by Codetector on 2016/11/13.
 */
@WebAPIImpl(prefix = "emoticon")
class EmoticonAPI : IWebAPIImpl {
    override fun initAPI(router: Router, sharedVertx: Vertx, dbClient: JDBCClient) {
        router.route().handler { ctx ->
            ctx.response().putHeader("Content-Type", "text/json; charset=utf-8")
            ctx.next()
        }
        router.route().handler {
            ctx ->
            ctx.response().end(JsonObject().put("emoticon", EmoticonManager.get()).toString())
        }
    }
}