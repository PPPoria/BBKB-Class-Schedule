package com.bbkb.sc.schedule.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteItemDao {

    @Insert
    fun insert(items: List<NoteItem>)

    @Update
    fun update(item: NoteItem)

    @Query("SELECT * FROM note_items")
    fun getAll(): Flow<List<NoteItem>>
}