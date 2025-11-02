package com.bbkb.sc.schedule.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bbkb.sc.SCApp

@Database(entities = [Problem::class], version = 1)
abstract class ProblemDB : RoomDatabase() {
    abstract fun dao(): ProblemDao

    companion object {
        @Volatile
        private var instance: ProblemDB? = null

        fun getInstance() = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                SCApp.app,
                ProblemDB::class.java,
                "problems"
            ).build().also { instance = it }
        }
    }
}