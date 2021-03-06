/*
 * Copyright (c) 2016. Codetector (Yaotian Feng)
 */

package cn.codetector.guardianCheck.server.data.students

/**
 * Created by Codetector on 22/11/2016.
 */
enum class StudentType(val value: Int) {
    Day(2),
    Boarding(1),
    Unknown(0);

    override fun toString(): String {
        return when (value) {
            Day.value -> "Day"
            Boarding.value -> "Boarding"
            else -> {
                "Unknown"
            }
        }
    }
}