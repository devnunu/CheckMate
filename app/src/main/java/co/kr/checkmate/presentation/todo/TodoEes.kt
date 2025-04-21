package co.kr.checkmate.presentation.todo

import co.kr.checkmate.ui.base.SideEffect
import co.kr.checkmate.ui.base.ViewEvent
import co.kr.checkmate.ui.base.ViewState
import org.threeten.bp.LocalDate

sealed class TodoViewEvent : ViewEvent {
    data class UpdateTitle(val title: String) : TodoViewEvent()
    data class SetDate(val date: LocalDate) : TodoViewEvent()
    data object SaveTodo : TodoViewEvent()
    data object Dismiss : TodoViewEvent()
}

sealed class TodoSideEffect : SideEffect {
    data object TodoSaved : TodoSideEffect()
    data object Dismissed : TodoSideEffect()
    data class ShowError(val message: String) : TodoSideEffect()
}

data class TodoState(
    val title: String = "",
    val date: LocalDate = LocalDate.now(),
    val isLoading: Boolean = false
) : ViewState