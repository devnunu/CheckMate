package co.kr.checkmate.domain.usecase

import co.kr.checkmate.domain.repository.TaskRepository

class ToggleTodoUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(todoId: Long) =
        repository.toggleTodo(todoId)
}