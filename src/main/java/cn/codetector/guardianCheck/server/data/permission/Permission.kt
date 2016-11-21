package cn.codetector.guardianCheck.server.data.permission

data class Permission (val id:Int, val name:String, val description:String = ""){
    override fun toString(): String {
        return this.name
    }
}