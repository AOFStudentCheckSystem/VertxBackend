package cn.codetector.guardianCheck.server.permission

import cn.codetector.guardianCheck.server.data.permission.Permission
import junit.framework.Assert
import junit.framework.TestCase
import java.util.*

class PermissionTest : TestCase() {
    fun testToString() {
        val testStr = UUID.randomUUID().toString()
        val per = Permission(testStr,"testDesc")
        Assert.assertEquals(testStr,per.toString())
    }

    fun testGetName() {
        val testStr = UUID.randomUUID().toString()
        val per = Permission(testStr,"testDesc")
        Assert.assertEquals(testStr,per.name)
    }

    fun testGetDescription() {
        val testStr = UUID.randomUUID().toString()
        val per = Permission("Test",testStr)
        Assert.assertEquals(testStr,per.description)
    }

}