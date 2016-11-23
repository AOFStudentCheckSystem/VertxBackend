package cn.codetector.guardianCheck.server.data.event

import cn.codetector.guardianCheck.server.data.AbstractDataService
import java.util.*

/**
 * Created by Codetector on 23/11/2016.
 */
object EventManager : AbstractDataService(){
    private val allActiveEvents:MutableMap<String, Event> = HashMap()

    override fun initialize() {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun saveToDatabase(action: () -> Unit) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun loadFromDatabase(action: () -> Unit) {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    private fun removeCompletedEvents(){
        val eventIterator = allActiveEvents.iterator()
        while (eventIterator.hasNext()){
            if (eventIterator.next().value.eventStatus.value >= EventStatus.Complete.value){
                eventIterator.remove()
            }
        }
    }
}