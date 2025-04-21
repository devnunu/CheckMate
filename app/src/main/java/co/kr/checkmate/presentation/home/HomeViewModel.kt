package co.kr.checkmate.presentation.home

import androidx.lifecycle.viewModelScope
import co.kr.checkmate.domain.usecase.DeleteTaskUseCase
import co.kr.checkmate.domain.usecase.GetTasksUseCase
import co.kr.checkmate.domain.usecase.ToggleTodoUseCase
import co.kr.checkmate.ui.base.BaseViewModel
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate

class HomeViewModel(
    private val getTasksUseCase: GetTasksUseCase,
    private val toggleTodoUseCase: ToggleTodoUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase
) : BaseViewModel<HomeState, HomeSideEffect>(HomeState()) {

    init {
        loadTasks(LocalDate.now())
    }

    fun onEvent(event: HomeViewEvent) {
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
                postSideEffect(HomeSideEffect.NavigateToAddTodo(event.date))
            }

            is HomeViewEvent.NavigateToAddMemo -> {
                postSideEffect(HomeSideEffect.NavigateToAddMemo(event.date))
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
}