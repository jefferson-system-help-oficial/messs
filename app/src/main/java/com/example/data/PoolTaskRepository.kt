package com.example.data

import kotlinx.coroutines.flow.Flow

class PoolTaskRepository(private val poolTaskDao: PoolTaskDao) {
    val allTasks: Flow<List<PoolTaskEntity>> = poolTaskDao.getAllTasks()

    fun getTasksByStatus(status: String): Flow<List<PoolTaskEntity>> = poolTaskDao.getTasksByStatus(status)

    suspend fun getTaskById(id: Long): PoolTaskEntity? = poolTaskDao.getTaskById(id)

    suspend fun insertTask(task: PoolTaskEntity): Long = poolTaskDao.insertTask(task)

    suspend fun updateTask(task: PoolTaskEntity) = poolTaskDao.updateTask(task)

    suspend fun deleteTask(id: Long) = poolTaskDao.deleteTaskById(id)

    val totalCount: Flow<Int> = poolTaskDao.getTotalTaskCount()
    fun countByStatus(status: String): Flow<Int> = poolTaskDao.getCountByStatus(status)
}
