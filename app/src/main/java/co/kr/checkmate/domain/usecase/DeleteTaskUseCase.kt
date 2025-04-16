package co.kr.checkmate.domain.usecase

import co.kr.checkmate.domain.repository.TaskRepository

class DeleteTaskUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(taskId: Long) =
        repository.deleteTask(taskId)
}