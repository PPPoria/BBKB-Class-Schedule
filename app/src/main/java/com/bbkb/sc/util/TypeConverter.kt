package com.bbkb.sc.util

import androidx.room.TypeConverter

class StringListConverter {
    @TypeConverter
    fun listToString(list: List<String>): String {
        return list.joinToString(separator = "=_=")
    }

    @TypeConverter
    fun stringToList(str: String): List<String> {
        return str.split("=_=")
    }
}