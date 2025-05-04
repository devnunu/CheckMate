package co.kr.checkmate.domain.usecase

import co.kr.checkmate.domain.model.Task
import co.kr.checkmate.domain.repository.TaskRepository
import kotlinx.coroutines.flow.Flow
import org.threeten.bp.LocalDate

class GetWeekTasksUseCase(private val repository: TaskRepository) {
    operator fun invoke(startDate: LocalDate, endDate: LocalDate): Flow<Map<LocalDate, List<Task>>> =
        repository.getTasksByWeek(startDate, endDate)
}