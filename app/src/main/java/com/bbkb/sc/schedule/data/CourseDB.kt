package com.bbkb.sc.schedule.data

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bbkb.sc.SCApp

@Database(entities = [Course::class], version = 1)
abstract class CourseDB : RoomDatabase(){
    abstract fun dao(): CourseDao

    companion object {
        @Volatile
        private var instance: CourseDB? = null

        fun get() = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                SCApp.app,
                CourseDB::class.java,
                "courses"
            ).build().also { instance = it }
        }
    }
}