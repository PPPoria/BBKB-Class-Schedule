package com.bbkb.sc.util

import androidx.room.TypeConverter

class StringListConverter {
    @TypeConverter
    fun listToString(list: List<String>): String {
        if (list.isEmpty()) return ""
        return list.joinToString(separator = "=_=")
    }

    @TypeConverter
    fun stringToList(str: String): List<String> {
        if (str.isEmpty()) return emptyList()
        return str.split("=_=")
    }
}