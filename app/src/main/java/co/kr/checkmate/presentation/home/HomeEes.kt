package co.kr.checkmate.presentation.home

import co.kr.checkmate.domain.model.Task
import co.kr.checkmate.ui.base.SideEffect
import co.kr.checkmate.ui.base.ViewEvent
import co.kr.checkmate.ui.base.ViewState
import co.kr.checkmate.ui.components.ModalState
import org.threeten.bp.LocalDate

sealed interface HomeBottomSheetTag {
    data object None : HomeBottomSheetTag
    data class Todo(val date: LocalDate) : HomeBottomSheetTag
    data class Memo(val date: LocalDate) : HomeBottomSheetTag
}

sealed class HomeViewEvent : ViewEvent {
    data class SelectDate(val date: LocalDate) : HomeViewEvent()
    data class ToggleTodo(val todoId: Long) : HomeViewEvent()
    data class DeleteTask(val taskId: Long) : HomeViewEvent()
    data object ToggleMonthCalendar : HomeViewEvent()
    data object ExpandFab : HomeViewEvent()
    data object CollapseFab : HomeViewEvent()
    data class NavigateToAddTodo(val date: LocalDate) : HomeViewEvent()
    data class NavigateToAddMemo(val date: LocalDate) : HomeViewEvent()
    data object OnClickCloseBottomSheet : HomeViewEvent()
}

sealed class HomeSideEffect : SideEffect {
    data class ShowSnackbar(val message: String) : HomeSideEffect()
}

data class HomeState(
    val selectedDate: LocalDate = LocalDate.now(),
    val tasks: List<Task> = emptyList(),
    val showMonthCalendar: Boolean = false,
    val isFabExpanded: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val bottomSheetState: ModalState<HomeBottomSheetTag> =
        ModalState.Closed(HomeBottomSheetTag.None)
) : ViewState