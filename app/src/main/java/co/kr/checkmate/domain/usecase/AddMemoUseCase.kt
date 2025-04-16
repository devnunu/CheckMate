package co.kr.checkmate.domain.usecase

import co.kr.checkmate.domain.model.Task
import co.kr.checkmate.domain.repository.TaskRepository

class AddMemoUseCase(private val repository: TaskRepository) {
    suspend operator fun invoke(memo: Task.Memo): Long =
        repository.addMemo(memo)
}