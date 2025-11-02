package com.bbkb.sc.schedule.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.bbkb.sc.schedule.database.Problem
import kotlinx.coroutines.flow.Flow

@Dao
interface ProblemDao {

    @Insert
    fun insert(problems: List<Problem>): List<Long>

    @Update
    fun update(problems: List<Problem>)

    @Delete
    fun delete(problems: List<Problem>)

    @Query("SELECT * FROM problems")
    fun getAll(): Flow<List<Problem>>

    @Query("SELECT * FROM problems WHERE id = :id")
    fun getById(id: Long): Flow<Problem>

    @Query("SELECT * FROM problems WHERE category = :category")
    fun getByCategory(category: String): Flow<List<Problem>>
}