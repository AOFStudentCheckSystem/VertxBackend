package cn.codetector.guardianCheck.server.webService

/**
 * Created by Codetector on 2016/11/12.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class WebAPIImpl(val prefix: String)