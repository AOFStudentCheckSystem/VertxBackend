package cn.codetector.guardianCheck.server.data.students

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

/**
 * Created by Codetector on 22/11/2016.
 */
internal class StudentTest {
    @Test
    fun equals() {
        val standardStu = Student("firstName","lastName","nickName","123456","rfid",StudentType.Day,"DORM","")
        val standardStuExactCopy = Student("firstName","lastName","nickName","123456","rfid",StudentType.Day,"DORM","")
        val standardSameStudentID = Student(firstName = "", lastName = "", nickName = "", studentId = "123456", rfid = "", studentType = StudentType.Day, dorm = "", grade = "")
        assertTrue(standardStu == standardStuExactCopy)
        assertTrue(standardStu == standardSameStudentID)
    }
}