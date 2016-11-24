/*
 * Copyright (c) 2016. Codetector (Yaotian Feng)
 */

package cn.codetector.guardianCheck.server.data.event

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class EventTest {
    @Test
    fun equals() {
        val event1 = Event("Test")
        val event2 = Event("Test")
        val event3 = Event("Test1")
        val event4 = Event("TeST")
        assertTrue(event1 == event2)
        assertTrue(event1 == event4)
        assertTrue(event2 == event4)
        assertFalse(event2 == event3)
        assertFalse(event1 == event3)
        assertFalse(event4 == event3)
    }

}