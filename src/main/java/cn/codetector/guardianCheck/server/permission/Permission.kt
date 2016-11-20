package cn.codetector.guardianCheck.server.permission

data class Permission (val id:Int, val name:String){
    override fun toString(): String {
        return this.name
    }
}