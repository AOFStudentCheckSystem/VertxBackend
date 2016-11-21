package cn.codetector.guardianCheck.server.data

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Created by Codetector on 20/11/2016.
 */
object DataService {
    val executors:ExecutorService = Executors.newSingleThreadExecutor()

    fun start(){

    }

    fun terminate(){
        executors.awaitTermination(3,TimeUnit.SECONDS)
        executors.shutdown()
    }

    fun isTerminated():Boolean{
        return executors.isTerminated
    }
}