/*
 * Copyright (c) 2016. Codetector (Yaotian Feng)
 */

package cn.codetector.guardianCheck.server.data.event

import io.vertx.core.json.JsonObject

data class Event(val eventId: String, var eventName: String, var eventTime: Long, var eventStatus: EventStatus) {
    constructor(eventId: String) : this(eventId, "", 0, EventStatus.Planned)

    override fun equals(other: Any?): Boolean {
        if (other is Event) {
            if (eventId.isNotBlank() && other.eventId.isNotBlank()) {
                return eventId.equals(other.eventId, ignoreCase = true)
            }
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = eventId.hashCode()
        result = 31 * result + eventStatus.hashCode()
        return result
    }

    fun toJsonString(): String {
        return JsonObject().put("eventId", eventId).put("eventName", eventName).put("eventTime", eventTime).put("eventStatus", eventStatus.value)
                .put("eventStatusDescription", eventStatus.toString()).toString()
    }

}