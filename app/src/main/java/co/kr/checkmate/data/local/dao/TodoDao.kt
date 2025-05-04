package co.kr.checkmate.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import co.kr.checkmate.data.local.entity.TaskEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TodoDao {
    @Query("SELECT * FROM todos WHERE date = :date ORDER BY id DESC")
    fun getTodosByDate(date: Long): Flow<List<TaskEntity.TodoEntity>>

    @Query("SELECT * FROM todos ORDER BY date DESC")
    fun getAllTodos(): Flow<List<TaskEntity.TodoEntity>>

    @Query("SELECT * FROM todos WHERE date BETWEEN :startDate AND :endDate ORDER BY id DESC")
    fun getTodosByDateRange(startDate: Long, endDate: Long): Flow<List<TaskEntity.TodoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(todo: TaskEntity.TodoEntity): Long

    @Update
    suspend fun update(todo: TaskEntity.TodoEntity)

    @Query("DELETE FROM todos WHERE id = :todoId")
    suspend fun delete(todoId: Long)

    @Query("UPDATE todos SET isCompleted = NOT isCompleted WHERE id = :todoId")
    suspend fun toggleCompleted(todoId: Long)
}