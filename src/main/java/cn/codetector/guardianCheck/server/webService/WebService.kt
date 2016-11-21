package cn.codetector.guardianCheck.server.webService

import cn.codetector.util.Configuration.Configuration
import cn.codetector.util.Configuration.ConfigurationManager
import io.vertx.core.Vertx
import io.vertx.core.http.HttpServer
import io.vertx.core.http.HttpServerOptions
import io.vertx.core.logging.LoggerFactory
import io.vertx.core.net.JksOptions
import io.vertx.ext.jdbc.JDBCClient
import io.vertx.ext.web.Router
import org.reflections.Reflections
import java.util.*

object WebService {

    private val logger = LoggerFactory.getLogger(this.javaClass)

    private var isServiceRunning = false

    private val config: Configuration = ConfigurationManager.getConfiguration("webConfig.json")
    private val useSSL = config.getBooleanValue("enableSSL", false)
    private val httpPort = config.getIntergerValue("serverPort", 8000)
    private val sslKeyStore = config.getStringValue("SSLKeystoreFile", "key.jks")
    private val sslPassword = config.getStringValue("SSLKeystorePassword", "password")

    private val serviceList: MutableList<IWebAPIImpl> = ArrayList<IWebAPIImpl>()

    private var server:HttpServer? = null

    init {
        registerProviders()
    }

    private fun registerProviders() {
        val reflections = Reflections("cn.codetector.guardianCheck.server.webService.implementations")
        val allAnnotatedClasses: Set<Class<*>> = reflections.getTypesAnnotatedWith(WebAPIImpl::class.java)
        allAnnotatedClasses.forEach {
            clazz ->
            if (IWebAPIImpl::class.java.isAssignableFrom(clazz)) {
                serviceList.add(clazz.newInstance() as IWebAPIImpl)
            }
        }
    }

    fun initService(sharedVertx: Vertx, jdbcClient: JDBCClient) {
        logger.info("Starting WebService...")
        val router = Router.router(sharedVertx)
        serviceList.forEach {
            serviceImpl ->
            var prefix = ""
            for (annotation in serviceImpl.javaClass.declaredAnnotations) {
                if (annotation is WebAPIImpl) {
                    prefix = annotation.prefix
                }
            }
            if (prefix == "") {
                serviceImpl.initAPI(router, sharedVertx, jdbcClient)
            } else {
                val subRouter: Router = Router.router(sharedVertx)
                serviceImpl.initAPI(subRouter, sharedVertx, jdbcClient)
                router.mountSubRouter("/" + prefix, subRouter)
            }
        }
        router.route().handler {
            ctx ->
            ctx.response().end("You should not land here.. Whoops!")
        }
        server = sharedVertx.createHttpServer(HttpServerOptions()
                .setSsl(useSSL)
                .setKeyStoreOptions(JksOptions()
                        .setPath(sslKeyStore)
                        .setPassword(sslPassword))
        )
        server!!.requestHandler{ context ->
            router.accept(context)
        }.listen(httpPort, {
            handler ->
            this.isServiceRunning = true;
            logger.info("WebService started at Port: $httpPort, SSL=$useSSL")
        })
    }

    fun shutdown(){
        if(server != null && isServiceRunning){
            server!!.close{ res ->
                if(res.succeeded()){
                    logger.info("HttpWebServer Shutdown")
                }else{
                    logger.error("Failed to shutdown WebServer",res.cause())
                }
            }
        }
    }
}