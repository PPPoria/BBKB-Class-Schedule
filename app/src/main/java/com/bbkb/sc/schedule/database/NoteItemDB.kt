package com.bbkb.sc.schedule.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.bbkb.sc.SCApp

@Database(entities = [NoteItem::class], version = 1)
abstract class NoteItemDB : RoomDatabase() {
    abstract fun dao(): NoteItemDao

    companion object {
        @Volatile
        private var instance: NoteItemDB? = null

        fun get() = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                SCApp.app,
                NoteItemDB::class.java,
                "note_items"
            ).build().also { instance = it }
        }
    }
}