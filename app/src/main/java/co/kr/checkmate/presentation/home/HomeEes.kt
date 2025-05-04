package co.kr.checkmate.presentation.home

import co.kr.checkmate.domain.model.Task
import co.kr.checkmate.ui.base.SideEffect
import co.kr.checkmate.ui.base.ViewEvent
import co.kr.checkmate.ui.base.ViewState
import co.kr.checkmate.ui.components.ModalState
import org.threeten.bp.LocalDate

sealed interface HomeBottomSheetTag {
    data object None : HomeBottomSheetTag
    data class Todo(val isEditMode: Boolean = false, val selectedTodo: Task.Todo? = null) : HomeBottomSheetTag
    data object Memo : HomeBottomSheetTag
}

sealed interface HomeDialogTag {
    data object MoveTodos : HomeDialogTag
}

sealed interface HomeViewEvent : ViewEvent {
    data class OnChangeSelectDate(val date: LocalDate) : HomeViewEvent
    data class OnToggleTodo(val todoId: Long) : HomeViewEvent
    data class OnSwipeDeleteTodo(val taskId: Long) : HomeViewEvent
    data object OnClickCalendarIcon : HomeViewEvent
    data object OnExpandFab : HomeViewEvent
    data object OnCollapseFab : HomeViewEvent
    data object OnClickAddMemoBtn : HomeViewEvent
    data object OnClickAddTodoBtn : HomeViewEvent

    // modal
    data object OnClickCloseBottomSheet : HomeViewEvent
    data object OnClickCloseDialog : HomeViewEvent

    // memo
    data class OnCreateMemo(val title: String, val content: String, val date: LocalDate? = null) : HomeViewEvent

    // td
    data class OnCreateTodo(val title: String, val date: LocalDate? = null) : HomeViewEvent
    data class OnSwipeEditTodo(val todo: Task.Todo) : HomeViewEvent
    data class OnClickDeleteTodo(val todoId: Long) : HomeViewEvent  // data class로 변경
    data class OnUpdateTodo(val todoId: Long, val title: String) : HomeViewEvent  // todoId 추가

    // move TD
    data object OnClickMoveTodosToToday : HomeViewEvent
    data object OnConfirmMoveTodosToToday : HomeViewEvent
}

sealed interface HomeSideEffect : SideEffect {
    data class ShowSnackbar(val message: String) : HomeSideEffect
    data object NavigateToCalendar : HomeSideEffect
}

data class HomeState(
    val isLoading: Boolean = false,
    val selectedDate: LocalDate = LocalDate.now(),
    val weekTasks: Map<LocalDate, List<Task>> = emptyMap(),
    val isFabExpanded: Boolean = false,
    val error: String? = null,
    // modal
    val bottomSheetState: ModalState<HomeBottomSheetTag> =
        ModalState.Closed(HomeBottomSheetTag.None),
    val dialogState: ModalState<HomeDialogTag> =
        ModalState.Closed(HomeDialogTag.MoveTodos)
) : ViewState {

    val tasks: List<Task>
        get() = weekTasks[selectedDate] ?: emptyList()

}