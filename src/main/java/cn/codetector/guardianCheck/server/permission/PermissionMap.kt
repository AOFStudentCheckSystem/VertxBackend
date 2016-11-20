package cn.codetector.guardianCheck.server.permission

import java.util.*

class PermissionMap : ArrayList<Permission>() {
    override fun add(element: Permission): Boolean {
        if (!this.contains(element)) {
            return super.add(element)
        }
        return false;
    }

    override fun addAll(elements: Collection<Permission>): Boolean {
        elements.forEach {
            element ->
            this.add(element)
        }
        return true;
    }
}