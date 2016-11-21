package cn.codetector.guardianCheck.server.data.user

import cn.codetector.guardianCheck.server.data.permission.Role

class User (val username:String, val passwordHash:String, val role: Role){

}