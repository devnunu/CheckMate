package co.kr.checkmate.domain.usecase

import co.kr.checkmate.domain.model.Task
import co.kr.checkmate.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import org.threeten.bp.LocalDate

class GetTasksUseCase(private val repository: TaskRepository) {
    operator fun invoke(date: LocalDate): Flow<List<Task>> =
        repository.getTasksByDate(date)
}