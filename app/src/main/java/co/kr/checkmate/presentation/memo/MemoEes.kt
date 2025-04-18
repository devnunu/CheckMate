package co.kr.checkmate.presentation.memo

import org.threeten.bp.LocalDate

sealed class MemoViewEvent {
    data class UpdateTitle(val title: String) : MemoViewEvent()
    data class UpdateContent(val content: String) : MemoViewEvent()
    data class SetDate(val date: LocalDate) : MemoViewEvent()
    data object SaveMemo : MemoViewEvent()
    data object Dismiss : MemoViewEvent()
}

sealed class MemoSideEffect {
    data object MemoSaved : MemoSideEffect()
    data object Dismissed : MemoSideEffect()
    data class ShowError(val message: String) : MemoSideEffect()
}

data class MemoState(
    val title: String = "",
    val content: String = "",
    val date: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false
)