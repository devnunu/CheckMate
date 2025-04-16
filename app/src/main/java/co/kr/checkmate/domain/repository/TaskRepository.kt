package co.kr.checkmate.domain.repository

import co.kr.checkmate.domain.model.Task
import kotlinx.coroutines.flow.Flow
import org.threeten.bp.LocalDate

interface TaskRepository {
    fun getTasksByDate(date: LocalDate): Flow<List<Task>>
    fun getAllTasks(): Flow<List<Task>>
    suspend fun addTodo(todo: Task.Todo): Long
    suspend fun addMemo(memo: Task.Memo): Long
    suspend fun toggleTodo(todoId: Long)
    suspend fun deleteTask(taskId: Long)
    suspend fun updateTodo(todo: Task.Todo)
    suspend fun updateMemo(memo: Task.Memo)
}