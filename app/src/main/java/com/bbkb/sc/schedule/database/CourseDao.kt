package com.bbkb.sc.schedule.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {

    @Insert
    fun insert(courses: List<Course>)

    @Update
    fun update(course: Course)

    @Query("SELECT * FROM courses")
    fun getAll(): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE zc = :zc")
    fun getByZC(zc: Int): Flow<List<Course>>

    @Query("SELECT * FROM courses WHERE name = :name")
    fun getByName(name: String): Flow<List<Course>>

    @Query("DELETE FROM courses")
    fun deleteAll()

    @Query("DELETE FROM courses WHERE zc = :zc")
    fun deleteByZC(zc: Int)
}