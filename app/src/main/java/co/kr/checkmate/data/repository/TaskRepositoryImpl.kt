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

    override fun getTasksByWeek(startDate: LocalDate, endDate: LocalDate): Flow<Map<LocalDate, List<Task>>> {
        val startTimestamp = startDate.toEpochDay()
        val endTimestamp = endDate.toEpochDay()

        return combine(
            todoDao.getTodosByDateRange(startTimestamp, endTimestamp),
            memoDao.getMemosByDateRange(startTimestamp, endTimestamp)
        ) { todos, memos ->
            val tasksByDate = mutableMapOf<LocalDate, MutableList<Task>>()

            // 먼저 날짜별 빈 리스트 초기화
            var currentDate = startDate
            while (!currentDate.isAfter(endDate)) {
                tasksByDate[currentDate] = mutableListOf()
                currentDate = currentDate.plusDays(1)
            }

            // 할 일 추가
            todos.forEach { todoEntity ->
                val date = LocalDate.ofEpochDay(todoEntity.date)
                tasksByDate[date]?.add(todoEntity.toDomain())
            }

            // 메모 추가
            memos.forEach { memoEntity ->
                val date = LocalDate.ofEpochDay(memoEntity.date)
                tasksByDate[date]?.add(memoEntity.toDomain())
            }

            // 각 날짜별로 정렬
            tasksByDate.forEach { (_, tasks) ->
                tasks.sortByDescending { it.id }
            }

            tasksByDate
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