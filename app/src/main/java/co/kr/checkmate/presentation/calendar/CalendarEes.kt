package co.kr.checkmate.presentation.calendar

import co.kr.checkmate.ui.base.SideEffect
import co.kr.checkmate.ui.base.ViewEvent
import co.kr.checkmate.ui.base.ViewState

sealed interface CalendarViewEvent : ViewEvent {
    data object OnClickBack : CalendarViewEvent
}

sealed interface CalendarSideEffect : SideEffect {
    data object PopBackStack : CalendarSideEffect
}

data class CalendarState(
    val temp: String = ""
) : ViewState {
}