package com.bbkb.sc.schedule.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bbkb.sc.SCApp

@Database(entities = [NoteCategory::class], version = 1)
abstract class NoteCategoryDB : RoomDatabase() {
    abstract fun dao(): NoteCategoryDao

    companion object {
        @Volatile
        private var instance: NoteCategoryDB? = null

        fun get() = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                SCApp.app,
                NoteCategoryDB::class.java,
                "note_categories"
            ).build().also { instance = it }
        }
    }
}