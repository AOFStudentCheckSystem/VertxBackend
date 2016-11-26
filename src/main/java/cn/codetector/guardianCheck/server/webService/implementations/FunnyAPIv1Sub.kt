package cn.codetector.guardianCheck.server.webService.implementations

import cn.codetector.guardianCheck.server.webService.IWebAPIImpl
import io.vertx.core.Vertx
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.web.Router

/**
 * Created by Codetector on 2016/11/13.
 */
class FunnyAPIv1Sub : IWebAPIImpl {
    override fun initAPI(router: Router, sharedVertx: Vertx, dbClient: JDBCClient) {

    }
}
