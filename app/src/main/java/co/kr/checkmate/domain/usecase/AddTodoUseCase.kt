package co.kr.checkmate.domain.usecase

import co.kr.checkmate.domain.model.Task
import co.kr.checkmate.domain.repository.TaskRepository

class AddTodoUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(todo: Task.Todo): Long =
        repository.addTodo(todo)
}