package cn.codetector.guardianCheck.server.webService.implementations

import cn.codetector.guardianCheck.server.data.user.UserHash
import cn.codetector.guardianCheck.server.data.user.UserManager
import cn.codetector.guardianCheck.server.webService.IWebAPIImpl
import cn.codetector.guardianCheck.server.webService.WebAPIImpl
import io.vertx.core.Vertx
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.web.Router

/**
 * Created by codetector on 21/11/2016.
 */
@WebAPIImpl(prefix = "v3")
class APIImplV3 : IWebAPIImpl {
    private val logger = LoggerFactory.getLogger("WebAPI v3")
    private val noAuthExceptions: Set<String> = hashSetOf("/v3/api/auth")
    override fun initAPI(router: Router, sharedVertx: Vertx, dbClient: JDBCClient) {
        //Pre-flight handler
        router.options("/api/*").handler { ctx ->
            ctx.response().putHeader("Access-Control-Allow-Origin", "*")
            ctx.response().putHeader("Access-Control-Allow-Methods", "GET, POST, OPTIONS")
            ctx.response().putHeader("Access-Control-Allow-Headers", "Authorization")
            ctx.response().end()
        }
        //General Handlers
        router.route("/api/*").handler { ctx ->
            var path = ctx.request().path()
            if (path.endsWith("/")) {
                path = path.substring(0, path.length - 1)
            }
            if (noAuthExceptions.contains(path)) {
                ctx.next()
            } else {
                val auth = ctx.request().getHeader("Authorization").replace("Bearer ", "")
                if (UserHash.isAuthKeyValid(auth)) {
                    ctx.setUser(UserHash.getUserByAuthKey(auth))
                    ctx.next()
                } else {
                    logger.info(ctx.request().getHeader("Authorization"))
                    ctx.fail(401)
                }
            }
        }
        router.route().handler { ctx ->
            ctx.response().putHeader("Content-Type", "text/json; charset=utf-8")
            ctx.next()
        }
        router.route("/api/*").handler { ctx ->
            ctx.response().putHeader("Access-Control-Allow-Origin", "*")
            ctx.next()
        }
        router.post("/api/*").handler { ctx ->
            ctx.request().isExpectMultipart = true
            ctx.request().endHandler { aVoid -> ctx.next() }
        }

        //Login auth handler
        router.post("/api/auth").handler { ctx ->
            if (UserManager.hasUser(ctx.request().getFormAttribute("username"))) {
                val user = UserManager.getUserByUsername(ctx.request().getFormAttribute("username"))
                if (user.authenticate(ctx.request().getFormAttribute("password"))) {
                    val hash = UserHash.createWebUser(user)
                    ctx.response().end(JsonObject().put("token", hash).toString())
                } else {
                    ctx.fail(401)
                }
            } else {
                ctx.fail(401)
            }
        }

        router.post("/api/test").handler { ctx ->
            ctx.response().end(ctx.user().principal().toString())
        }
    }
}