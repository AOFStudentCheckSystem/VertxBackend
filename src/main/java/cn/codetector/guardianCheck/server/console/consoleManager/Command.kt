package cn.codetector.guardianCheck.server.console.consoleManager

@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Command (val command:String)