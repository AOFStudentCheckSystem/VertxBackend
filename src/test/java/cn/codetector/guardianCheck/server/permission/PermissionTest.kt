package cn.codetector.guardianCheck.server.permission

import cn.codetector.guardianCheck.server.data.permission.Permission
import junit.framework.Assert
import junit.framework.TestCase
import java.util.*

class PermissionTest : TestCase() {
    fun testToString() {
        val testStr = UUID.randomUUID().toString()
        val per = Permission(0,testStr,"testDesc")
        Assert.assertEquals(testStr,per.toString())
    }

    fun testGetId() {
        val id = Random().nextInt()
        val per = Permission(id,"","testDesc")
        Assert.assertEquals(id,per.id)
    }

    fun testGetName() {
        val testStr = UUID.randomUUID().toString()
        val per = Permission(0,testStr,"testDesc")
        Assert.assertEquals(testStr,per.name)
    }

    fun testGetDescription() {
        val testStr = UUID.randomUUID().toString()
        val per = Permission(0,"Test",testStr)
        Assert.assertEquals(testStr,per.description)
    }

}