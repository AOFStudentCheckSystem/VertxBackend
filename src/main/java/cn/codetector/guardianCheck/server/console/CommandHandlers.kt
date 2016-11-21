package cn.codetector.guardianCheck.server.console

import cn.codetector.guardianCheck.server.Main
import cn.codetector.guardianCheck.server.console.consoleManager.Command
import cn.codetector.guardianCheck.server.console.consoleManager.ConsoleManager
import cn.codetector.guardianCheck.server.webService.WebService

/**
 * Created by codetector on 20/11/2016.
 */
object CommandHandlers {
    @Command(command="web")
    fun webServiceCommandHandler(args: Array<String>):Boolean{
        if (args.size == 2){
            when(args[1]){
                "status" -> {
                    println("Web Service is currently " + if (WebService.isServiceRunning) "Running" else "Stopped")
                    return true
                }
                "stop" -> {
                    WebService.shutdown()
                    return true
                }
                "start" -> {
                    WebService.initService(Main.sharedVertx, Main.sharedJDBCClient)
                    return true
                }
            }
        }
        println("Available actions: (status)")
        return false
    }

    @Command(command="server")
    fun serverCommandHandler(args: Array<String>):Boolean{
        if (args.size == 2){
            when(args[1]){
                "stop" -> {
                    Main.stopService()
                    return true
                }
            }
        }
        println("Available actions: (status)")
        return false
    }

}