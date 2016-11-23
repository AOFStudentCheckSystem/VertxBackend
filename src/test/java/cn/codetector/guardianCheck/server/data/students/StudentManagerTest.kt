package cn.codetector.guardianCheck.server.data.students

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import java.lang.reflect.Field

class StudentManagerTest {
    @BeforeEach
    fun clearStudentManager(){
        val allStu:Field = StudentManager.javaClass.getDeclaredField("allStudents")
        allStu.isAccessible = true;
        (allStu.get(StudentManager) as MutableSet<*>).clear()
    }

    @Test
    fun testFindStudentById() {
        val stu = Student("123456")
        StudentManager.addOrUpdateStudent(stu)
        assertTrue(stu === StudentManager.findStudentById("123456"))
        assertNull(StudentManager.findStudentById("12345"))
    }
    @Test
    fun testContainsStudent(){
        StudentManager.addOrUpdateStudent(Student("123456789"))
        assertTrue(StudentManager.containsStudent(Student("123456789")))
        assertFalse(StudentManager.containsStudent(Student("123456")))
    }

}