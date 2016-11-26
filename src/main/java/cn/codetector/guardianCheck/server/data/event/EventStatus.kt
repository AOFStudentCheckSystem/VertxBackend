/*
 * Copyright (c) 2016. Codetector (Yaotian Feng)
 */

package cn.codetector.guardianCheck.server.data.event

/**
 * Created by Codetector on 23/11/2016.
 */
enum class EventStatus(val value: Int) {
    CheckedOut(-1),
    Planned(0),
    Boarding(1),
    Complete(2);

    override fun toString(): String {
        return when (value) {
            -1 -> "CheckedOut"
            0 -> "Planned"
            1 -> "Boarding"
            2 -> "Complete"
            else -> {
                "Unknown"
            }
        }
    }
}