package com.bbkb.sc.schedule.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RemarkDao {
    @Insert
    fun insert(remark: Remark)

    @Update
    fun update(remark: Remark)

    @Query("SELECT * FROM remarks")
    fun getAll(): Flow<List<Remark>>

    @Query("SELECT * FROM remarks WHERE courseName = :courseName")
    fun getByCourseName(courseName: String): Flow<List<Remark>>

    @Delete
    fun delete(remark: Remark)
}