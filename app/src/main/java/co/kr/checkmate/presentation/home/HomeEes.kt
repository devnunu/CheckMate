package co.kr.checkmate.presentation.home

import co.kr.checkmate.domain.model.Task
import org.threeten.bp.LocalDate

sealed class HomeViewEvent {
    data class SelectDate(val date: LocalDate) : HomeViewEvent()
    data class ToggleTodo(val todoId: Long) : HomeViewEvent()
    data class DeleteTask(val taskId: Long) : HomeViewEvent()
    data object ToggleMonthCalendar : HomeViewEvent()
    data object ExpandFab : HomeViewEvent()
    data object CollapseFab : HomeViewEvent()
    data class NavigateToAddTodo(val date: LocalDate) : HomeViewEvent()
    data class NavigateToAddMemo(val date: LocalDate) : HomeViewEvent()
}

sealed class HomeSideEffect {
    data class ShowSnackbar(val message: String) : HomeSideEffect()
    data class NavigateToAddTodo(val date: LocalDate) : HomeSideEffect()
    data class NavigateToAddMemo(val date: LocalDate) : HomeSideEffect()
}

data class HomeState(
    val selectedDate: LocalDate = LocalDate.now(),
    val tasks: List<Task> = emptyList(),
    val showMonthCalendar: Boolean = false,
    val isFabExpanded: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null
)