package cn.codetector.guardianCheck.server.data.students

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * Created by Codetector on 22/11/2016.
 */
class StudentManagerTest {
    @Test
    fun testFindStudentById() {

    }
    @Test
    fun testContainsStudent(){
        StudentManager.addOrUpdateStudent(Student("123456789"))
        assertTrue(StudentManager.containsStudent(Student("123456789")))
    }

}