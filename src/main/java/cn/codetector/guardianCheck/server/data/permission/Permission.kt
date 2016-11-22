package cn.codetector.guardianCheck.server.data.permission

data class Permission(val name: String, val description: String = "") {
    override fun toString(): String {
        return this.name
    }
}