package cn.codetector.guardianCheck.server.data.students

data class Student(val firstName: String, val lastName: String, val nickName: String, val studentId: String, val rfid: String, val studentType: StudentType, val dorm: String) {
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
}