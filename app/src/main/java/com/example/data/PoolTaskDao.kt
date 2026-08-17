package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PoolTaskDao {
    @Query("SELECT * FROM pool_tasks ORDER BY date DESC, timeSlot ASC")
    fun getAllTasks(): Flow<List<PoolTaskEntity>>

    @Query("SELECT * FROM pool_tasks WHERE status = :status ORDER BY date DESC")
    fun getTasksByStatus(status: String): Flow<List<PoolTaskEntity>>

    @Query("SELECT * FROM pool_tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): PoolTaskEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: PoolTaskEntity): Long

    @Update
    suspend fun updateTask(task: PoolTaskEntity)

    @Query("DELETE FROM pool_tasks WHERE id = :id")
    suspend fun deleteTaskById(id: Long)

    @Query("SELECT COUNT(*) FROM pool_tasks")
    fun getTotalTaskCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM pool_tasks WHERE status = :status")
    fun getCountByStatus(status: String): Flow<Int>
}
