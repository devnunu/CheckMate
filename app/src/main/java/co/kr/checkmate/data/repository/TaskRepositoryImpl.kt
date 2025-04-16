package co.kr.checkmate.data.repository

import co.kr.checkmate.data.local.dao.MemoDao
import co.kr.checkmate.data.local.dao.TodoDao
import co.kr.checkmate.data.local.entity.toEntity
import co.kr.checkmate.domain.model.Task
import co.kr.checkmate.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.threeten.bp.LocalDate

class TaskRepositoryImpl(
    private val todoDao: TodoDao,
    private val memoDao: MemoDao
) : TaskRepository {
    override fun getTasksByDate(date: LocalDate): Flow<List<Task>> {
        val dateTimestamp = date.toEpochDay()
        return combine(
            todoDao.getTodosByDate(dateTimestamp),
            memoDao.getMemosByDate(dateTimestamp)
        ) { todos, memos ->
            val taskList = mutableListOf<Task>()

            todos.forEach { todoEntity ->
                taskList.add(todoEntity.toDomain())
            }

            memos.forEach { memoEntity ->
                taskList.add(memoEntity.toDomain())
            }

            taskList.sortedByDescending { it.id }
        }
    }

    override fun getAllTasks(): Flow<List<Task>> {
        return combine(
            todoDao.getAllTodos(),
            memoDao.getAllMemos()
        ) { todos, memos ->
            val taskList = mutableListOf<Task>()

            todos.forEach { todoEntity ->
                taskList.add(todoEntity.toDomain())
            }

            memos.forEach { memoEntity ->
                taskList.add(memoEntity.toDomain())
            }

            taskList.sortedByDescending { it.id }
        }
    }

    override suspend fun addTodo(todo: Task.Todo): Long {
        return todoDao.insert(todo.toEntity())
    }

    override suspend fun addMemo(memo: Task.Memo): Long {
        return memoDao.insert(memo.toEntity())
    }

    override suspend fun toggleTodo(todoId: Long) {
        todoDao.toggleCompleted(todoId)
    }

    override suspend fun deleteTask(taskId: Long) {
        todoDao.delete(taskId)
        memoDao.delete(taskId)
    }

    override suspend fun updateTodo(todo: Task.Todo) {
        todoDao.update(todo.toEntity())
    }

    override suspend fun updateMemo(memo: Task.Memo) {
        memoDao.update(memo.toEntity())
    }
}