package cn.codetector.guardianCheck.server.data.students

import io.vertx.core.json.JsonObject

data class Student(var firstName: String, var lastName: String, var nickName: String, val studentId: String, var rfid: String, var studentType: StudentType, var dorm: String, var grade:String) {
    constructor(studentId: String):this("","","",studentId,"",StudentType.Unknown,"","")
    val fullName: String
        get() {
            return "$lastName $firstName"
        }
    override fun equals(other: Any?): Boolean {
        if (other is Student){
            if (studentId.isNotBlank() && other.studentId.isNotBlank()){
                return studentId.equals(other = other.studentId,ignoreCase = true)
            }
        }
        return super.equals(other)
    }

    override fun hashCode(): Int {
        var result = firstName.hashCode()
        result = 31 * result + lastName.hashCode()
        result = 31 * result + nickName.hashCode()
        result = 31 * result + studentId.hashCode()
        result = 31 * result + rfid.hashCode()
        result = 31 * result + studentType.hashCode()
        result = 31 * result + dorm.hashCode()
        return result
    }

    fun toJsonString():String{
        return JsonObject().put("firstName",firstName).put("lastName",lastName).put("nickName",nickName).put("studentId",studentId).
                put("rfid",rfid).put("type",studentType.value).put("typeDescription",studentType.toString()).put("dorm",dorm).
                put("grade",grade).toString()
    }
}