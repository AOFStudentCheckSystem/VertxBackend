package cn.codetector.guardianCheck.server.data.students

import java.util.*

object StudentManager {
    private val allStudents:MutableSet<Student> = HashSet()

    fun findStudentById(studentID:String): Student?{
        return allStudents.find { stu ->
            stu.studentId.equals(other = studentID, ignoreCase = true)
        }
    }

    fun addOrUpdateStudent(student: Student){
        if (allStudents.contains(student)){
            allStudents.remove(student)
        }
        allStudents.add(student)
    }

    fun removeStudent(student: Student){
        allStudents.remove(student)
    }

    fun containsStudent(student: Student):Boolean{
        return allStudents.contains(student)
    }

}