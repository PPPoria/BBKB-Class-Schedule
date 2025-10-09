package com.bbkb.sc.schedule.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteItemDao {

    @Insert
    fun insert(item: NoteItem): Long

    @Insert
    fun insert(items: List<NoteItem>): List<Long>

    @Update
    fun update(item: NoteItem)

    @Update
    fun update(items: List<NoteItem>)

    @Query("SELECT * FROM note_items")
    fun getAll(): Flow<List<NoteItem>>

    @Query("SELECT * FROM note_items WHERE categoryId = :categoryId")
    fun getByCategoryId(categoryId: Long): Flow<List<NoteItem>>

    @Delete
    fun delete(item: NoteItem)

    @Query("DELETE FROM note_items WHERE categoryId = :categoryId")
    fun deleteByCategoryId(categoryId: Long)
}