package co.kr.checkmate.presentation.home

import androidx.lifecycle.viewModelScope
import co.kr.checkmate.domain.model.Task
import co.kr.checkmate.domain.usecase.AddMemoUseCase
import co.kr.checkmate.domain.usecase.AddTodoUseCase
import co.kr.checkmate.domain.usecase.DeleteTaskUseCase
import co.kr.checkmate.domain.usecase.GetTasksUseCase
import co.kr.checkmate.domain.usecase.ToggleTodoUseCase
import co.kr.checkmate.ui.base.BaseViewModel
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate

class HomeViewModel(
    private val getTasksUseCase: GetTasksUseCase,
    private val toggleTodoUseCase: ToggleTodoUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val addMemoUseCase: AddMemoUseCase,
    private val addTodoUseCase: AddTodoUseCase
) : BaseViewModel<HomeState, HomeViewEvent, HomeSideEffect>(
    initialState = HomeState()
) {

    init {
        loadTasks(LocalDate.now())
    }

    override fun onEvent(event: HomeViewEvent) {
        when (event) {
            is HomeViewEvent.SelectDate -> {
                setState { copy(selectedDate = event.date) }
                loadTasks(event.date)
            }

            is HomeViewEvent.ToggleTodo -> {
                viewModelScope.launch {
                    try {
                        toggleTodoUseCase(event.todoId)
                    } catch (e: Exception) {
                        postSideEffect(HomeSideEffect.ShowSnackbar("할 일 상태 변경에 실패했습니다."))
                    }
                }
            }

            is HomeViewEvent.DeleteTask -> {
                viewModelScope.launch {
                    try {
                        deleteTaskUseCase(event.taskId)
                        postSideEffect(HomeSideEffect.ShowSnackbar("항목이 삭제되었습니다."))
                    } catch (e: Exception) {
                        postSideEffect(HomeSideEffect.ShowSnackbar("삭제에 실패했습니다."))
                    }
                }
            }

            is HomeViewEvent.ToggleMonthCalendar -> {
                setState { copy(showMonthCalendar = !showMonthCalendar) }
            }

            is HomeViewEvent.ExpandFab -> {
                setState { copy(isFabExpanded = true) }
            }

            is HomeViewEvent.CollapseFab -> {
                setState { copy(isFabExpanded = false) }
            }

            is HomeViewEvent.NavigateToAddTodo -> {
                openBottomSheet(HomeBottomSheetTag.Todo(event.date))
            }

            is HomeViewEvent.NavigateToAddMemo -> {
                openBottomSheet(HomeBottomSheetTag.Memo(event.date))
            }

            is HomeViewEvent.OnClickCloseBottomSheet -> {
                closeBottomSheet()
            }

            is HomeViewEvent.SetDate -> {
                setState { copy(selectedDate = event.date) }
            }
            is HomeViewEvent.OnCreateMemo -> saveMemo(event.title, event.content)
            is HomeViewEvent.OnChangeTodoTitle -> {
                setState { copy(editTodoTitle = event.title) }
            }

            is HomeViewEvent.OnChangeTodoDate -> {
                setState { copy(selectedDate = event.date) }
            }

            is HomeViewEvent.OnUpdateTodo -> saveTodo()
        }
    }

    private fun saveTodo() {
        if (state.editTodoTitle.isBlank()) {
//            postSideEffect(TodoSideEffect.ShowError("제목을 입력해주세요."))
            return
        }

        viewModelScope.launch {
            setState { copy(isLoading = true) }
            try {
                val todo = Task.Todo(
                    title = state.editTodoTitle,
                    date = state.selectedDate,
                    isCompleted = false
                )
                addTodoUseCase(todo)
                closeBottomSheet()
            } catch (e: Exception) {
//                postSideEffect(TodoSideEffect.ShowError("저장에 실패했습니다."))
            } finally {
                setState { copy(isLoading = false) }
            }
        }
    }

    private fun saveMemo(title: String, content: String) {
        if (title.isBlank()) {
//            postSideEffect(MemoSideEffect.ShowError("제목을 입력해주세요."))
            return
        }

        viewModelScope.launch {
            setState { copy(isLoading = true) }
            try {
                val memo = Task.Memo(
                    title = title,
                    content = content,
                    date = state.selectedDate
                )
                addMemoUseCase(memo)
                closeBottomSheet()
            } catch (e: Exception) {
//                postSideEffect(MemoSideEffect.ShowError("메모 저장에 실패했습니다."))
            } finally {
                setState { copy(isLoading = false) }
            }
        }
    }

    private fun loadTasks(date: LocalDate) {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            try {
                getTasksUseCase(date).collect { tasks ->
                    setState {
                        copy(tasks = tasks, isLoading = false)
                    }
                }
            } catch (e: Exception) {
                setState {
                    copy(isLoading = false, error = "태스크를 불러오는데 실패했습니다.")
                }
            }
        }
    }

    /**
     * Modal
     * */
    private fun openBottomSheet(tag: HomeBottomSheetTag) {
        setState { copy(bottomSheetState = bottomSheetState.open(tag)) }
    }

    private fun closeBottomSheet() {
        setState { copy(bottomSheetState = bottomSheetState.close()) }
    }
}