/*
 * Copyright (c) 2016. Codetector (Yaotian Feng)
 */

package cn.codetector.guardianCheck.server.data.students

import cn.codetector.guardianCheck.server.data.AbstractDataService
import cn.codetector.guardianCheck.server.data.user.UserManager
import io.vertx.core.json.JsonArray
import io.vertx.core.json.JsonObject
import io.vertx.core.logging.LoggerFactory
import java.util.*

object StudentManager : AbstractDataService() {
    private val logger = LoggerFactory.getLogger(this.javaClass)
    private val allStudents: MutableSet<Student> = HashSet()

    val NO_RFID_FILTER: (Student) -> (Boolean) = { student: Student ->
        student.rfid.isNotBlank()
    }

    fun findStudentById(studentID: String): Student? {
        return allStudents.find { stu ->
            stu.studentId.equals(other = studentID, ignoreCase = true)
        }
    }

    fun addOrUpdateStudent(student: Student) {
        if (allStudents.contains(student)) {
            allStudents.remove(student)
        }
        allStudents.add(student)
    }

    fun removeStudent(student: Student) {
        allStudents.remove(student)
    }

    fun containsStudent(student: Student): Boolean {
        return allStudents.contains(student)
    }

    fun countStudent(): Int {
        return allStudents.size
    }

    fun countStudent(filter: (student: Student) -> (Boolean)): Int {
        return allStudents.filter(filter).size
    }

    fun allStudentsAsJsonArray(): String {
        val jsonArray = JsonArray()
        allStudents.forEach { student ->
            jsonArray.add(JsonObject(student.toJsonString()))
        }
        return jsonArray.toString()
    }

    override fun initialize() {
        logger.info("Student Manager Initialized")
    }

    override fun saveToDatabase(action: () -> Unit) {
        if (isInitialized()) {
            UserManager.logger.trace("Saving students to database...")
            dbClient!!.getConnection { conn ->
                val students: MutableList<JsonArray> = ArrayList()
                this.allStudents.forEach { stu ->
                    students.add(JsonArray().add(stu.studentId).add(stu.firstName)
                            .add(stu.lastName).add(stu.nickName).add(stu.rfid)
                            .add(stu.grade).add(stu.studentType.value).add(stu.dorm))
                }
                if (conn.succeeded()) {
                    conn.result().batchWithParams("INSERT INTO `dummystudent` (`studentID`, `firstName`, `lastName`, `nickName`, `rfid`, `grade`, `type`, `dorm`) VALUES (?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE `firstName` = VALUES(`firstName`),`lastName` = VALUES(`lastName`),`nickName` = VALUES(`nickName`),`rfid` = VALUES(`rfid`),`grade` = VALUES(`grade`),`type` = VALUES(`type`),`dorm` = VALUES(`dorm`)", students, {
                        result ->
                        if (result.succeeded()) {
                            var success = 0
                            var fail = 0
                            result.result().forEach {
                                if (it == 1) success++ else fail++
                            }
                            UserManager.logger.trace("Students save complete, Succeed:$success, Fail:$fail")
                        }
                        action.invoke()
                    })
                } else {
                    UserManager.logger.warn("Failed to initialize database connection", conn.cause())
                    action.invoke()
                }
            }
        } else {
            throw IllegalStateException("Student Manager Service have not been initialized yet.")
        }
    }

    override fun loadFromDatabase(action: () -> Unit) {
        logger.trace("Updating all student data from database...")
        if (isInitialized()) {
            dbClient!!.getConnection { conn ->
                if (conn.succeeded()) {
                    conn.result().query("SELECT * FROM `dummystudent`", { result ->
                        if (result.succeeded()) {
                            allStudents.clear()
                            result.result().rows.forEach { row ->
                                allStudents.add(Student(firstName = row.getString("firstName"), lastName = row.getString("lastName"), grade = row.getString("grade"),
                                        nickName = row.getString("nickName"), studentId = row.getString("studentID"), dorm = row.getString("dorm"),
                                        rfid = row.getString("rfid"), studentType = if (row.getInteger("type") == 1) StudentType.Boarding else StudentType.Day))
                            }
                            logger.trace("All (${allStudents.size}) Students loaded.")
                        } else {
                            logger.error("Failed to execute student select query", result.cause())
                        }
                        action.invoke()
                    })
                } else {
                    action.invoke()
                    logger.error("Failed to obtain Database connection", conn.cause())
                }
            }
        } else {
            throw IllegalStateException("Student Manager Service have not been initialized yet.")
        }
    }

}