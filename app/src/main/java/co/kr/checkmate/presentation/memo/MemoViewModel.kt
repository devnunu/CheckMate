package co.kr.checkmate.presentation.memo

import androidx.lifecycle.viewModelScope
import co.kr.checkmate.domain.model.Task
import co.kr.checkmate.domain.usecase.AddMemoUseCase
import co.kr.checkmate.ui.base.BaseViewModel
import kotlinx.coroutines.launch

class MemoViewModel(
    private val addMemoUseCase: AddMemoUseCase
) : BaseViewModel<MemoState, MemoSideEffect>(MemoState()) {

    fun processEvent(event: MemoViewEvent) {
        when (event) {
            is MemoViewEvent.UpdateTitle -> {
                setState { copy(title = event.title) }
            }
            is MemoViewEvent.UpdateContent -> {
                setState { copy(content = event.content) }
            }
            is MemoViewEvent.SetDate -> {
                setState { copy(date = event.date) }
            }
            is MemoViewEvent.SaveMemo -> saveMemo()
            is MemoViewEvent.Dismiss -> {
                postSideEffect(MemoSideEffect.Dismissed)
            }
        }
    }

    private fun saveMemo() {
        val currentState = state.value

        if (currentState.title.isBlank()) {
            postSideEffect(MemoSideEffect.ShowError("제목을 입력해주세요."))
            return
        }

        viewModelScope.launch {
            setState { copy(isLoading = true) }
            try {
                val memo = Task.Memo(
                    title = currentState.title,
                    content = currentState.content,
                    date = currentState.date
                )
                addMemoUseCase(memo)
                postSideEffect(MemoSideEffect.MemoSaved)
            } catch (e: Exception) {
                postSideEffect(MemoSideEffect.ShowError("메모 저장에 실패했습니다."))
            } finally {
                setState { copy(isLoading = false) }
            }
        }
    }
}