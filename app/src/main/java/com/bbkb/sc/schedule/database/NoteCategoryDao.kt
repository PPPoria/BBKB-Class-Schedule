package com.bbkb.sc.schedule.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteCategoryDao {

    @Insert
    fun insert(categories: List<NoteCategory>)

    @Update
    fun update(category: NoteCategory)

    @Query("SELECT * FROM note_categories")
    fun getAll(): Flow<List<NoteCategory>>
}