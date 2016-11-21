package cn.codetector.guardianCheck.server.console

import cn.codetector.guardianCheck.server.console.consoleManager.Command

/**
 * Created by codetector on 20/11/2016.
 */
object CommandHandlers {
    @Command(command="test")
    fun testCommandHandler(args: Array<String>):Boolean{
        return true
    }

}