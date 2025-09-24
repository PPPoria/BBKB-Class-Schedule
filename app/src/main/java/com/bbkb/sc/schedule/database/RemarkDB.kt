package com.bbkb.sc.schedule.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bbkb.sc.SCApp

@Database(entities = [Remark::class], version = 1)
abstract class RemarkDB : RoomDatabase() {
    abstract fun dao(): RemarkDao

    companion object {
        @Volatile
        private var instance: RemarkDB? = null

        fun get() = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                SCApp.app,
                RemarkDB::class.java,
                "remarks"
            ).build().also { instance = it }
        }
    }
}