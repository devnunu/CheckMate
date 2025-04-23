package co.kr.checkmate.presentation.calendar

import co.kr.checkmate.ui.base.BaseViewModel

class CalendarViewModel : BaseViewModel<CalendarState, CalendarViewEvent, CalendarSideEffect>(
    initialState = CalendarState()
) {


    override fun onEvent(event: CalendarViewEvent) {
        when (event) {
            is CalendarViewEvent.OnClickBack -> {
                postSideEffect(CalendarSideEffect.PopBackStack)
            }
        }
    }
}