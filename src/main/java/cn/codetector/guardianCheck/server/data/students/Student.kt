package cn.codetector.guardianCheck.server.data.students

data class Student(val firstName: String, val lastName: String, val nickName: String, val studentId: String, val rfid: String, val studentType: StudentType, val dorm: String) {
    constructor(studentId: String):this("","","",studentId,"",StudentType.Unknown,"")
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
}