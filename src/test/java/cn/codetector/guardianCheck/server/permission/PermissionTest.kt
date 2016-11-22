package cn.codetector.guardianCheck.server.permission

import cn.codetector.guardianCheck.server.data.permission.Permission
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.util.*


class PermissionTest{
    @Test
    fun testToString() {
        val testStr = UUID.randomUUID().toString()
        val per = Permission(testStr, "testDesc")
        assertEquals(testStr, per.toString())
    }
    @Test
    fun testGetName() {
        val testStr = UUID.randomUUID().toString()
        val per = Permission(testStr, "testDesc")
        assertEquals(testStr, per.name)
    }
    @Test
    fun testGetDescription() {
        val testStr = UUID.randomUUID().toString()
        val per = Permission("Test", testStr)
        assertEquals(testStr, per.description)
    }

}