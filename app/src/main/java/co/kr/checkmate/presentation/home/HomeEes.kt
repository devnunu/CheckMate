package co.kr.checkmate.presentation.home

import co.kr.checkmate.domain.model.Task
import co.kr.checkmate.ui.base.SideEffect
import co.kr.checkmate.ui.base.ViewEvent
import co.kr.checkmate.ui.base.ViewState
import co.kr.checkmate.ui.components.ModalState
import org.threeten.bp.LocalDate

sealed interface HomeBottomSheetTag {
    data object None : HomeBottomSheetTag
    data object Todo : HomeBottomSheetTag
    data object Memo : HomeBottomSheetTag
}

sealed class HomeViewEvent : ViewEvent {
    data class OnChangeSelectDate(val date: LocalDate) : HomeViewEvent()
    data class OnToggleTodo(val todoId: Long) : HomeViewEvent()
    data class OnDeleteTask(val taskId: Long) : HomeViewEvent()
    data object OnToggleMonthCalendar : HomeViewEvent()
    data object OnExpandFab : HomeViewEvent()
    data object OnCollapseFab : HomeViewEvent()
    data object OnClickAddTodoBtn : HomeViewEvent()
    data object OnClickAddMemoBtn : HomeViewEvent()

    // modal
    data object OnClickCloseBottomSheet : HomeViewEvent()

    // memo
    data class OnChangeMemoDate(val date: LocalDate) : HomeViewEvent()
    data class OnCreateMemo(val title: String, val content: String) : HomeViewEvent()

    // td
    data class OnChangeTodoDate(val date: LocalDate) : HomeViewEvent()
    data class OnCreateTodo(val title: String) : HomeViewEvent()
}

sealed class HomeSideEffect : SideEffect {
    data class ShowSnackbar(val message: String) : HomeSideEffect()
}

data class HomeState(
    val isLoading: Boolean = false,

    val selectedDate: LocalDate = LocalDate.now(),
    val tasks: List<Task> = emptyList(),
    val showMonthCalendar: Boolean = false,
    val isFabExpanded: Boolean = false,
    val error: String? = null,
    // modal
    val bottomSheetState: ModalState<HomeBottomSheetTag> =
        ModalState.Closed(HomeBottomSheetTag.None)
) : ViewState