package co.kr.checkmate.presentation.home

import androidx.lifecycle.viewModelScope
import co.kr.checkmate.domain.model.Task
import co.kr.checkmate.domain.usecase.AddMemoUseCase
import co.kr.checkmate.domain.usecase.AddTodoUseCase
import co.kr.checkmate.domain.usecase.DeleteTaskUseCase
import co.kr.checkmate.domain.usecase.GetTasksUseCase
import co.kr.checkmate.domain.usecase.ToggleTodoUseCase
import co.kr.checkmate.domain.usecase.UpdateTodoUseCase
import co.kr.checkmate.ui.base.BaseViewModel
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate

class HomeViewModel(
    private val getTasksUseCase: GetTasksUseCase,
    private val toggleTodoUseCase: ToggleTodoUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val addMemoUseCase: AddMemoUseCase,
    private val addTodoUseCase: AddTodoUseCase,
    private val updateTodoUseCase: UpdateTodoUseCase
) : BaseViewModel<HomeState, HomeViewEvent, HomeSideEffect>(
    initialState = HomeState()
) {

    init {
        loadTasks(LocalDate.now())
    }

    override fun onEvent(event: HomeViewEvent) {
        when (event) {
            is HomeViewEvent.OnChangeSelectDate -> handleChangeSelectDate(event)
            is HomeViewEvent.OnToggleTodo -> handleToggleTodo(event)
            is HomeViewEvent.OnDeleteTask -> handleDeleteTask(event)
            is HomeViewEvent.OnClickCalendarIcon -> postSideEffect(HomeSideEffect.NavigateToCalendar)
            is HomeViewEvent.OnExpandFab -> setState { copy(isFabExpanded = true) }
            is HomeViewEvent.OnCollapseFab -> setState { copy(isFabExpanded = false) }
            is HomeViewEvent.OnClickAddTodoBtn -> openBottomSheet(HomeBottomSheetTag.Todo)
            is HomeViewEvent.OnClickAddMemoBtn -> openBottomSheet(HomeBottomSheetTag.Memo)
            is HomeViewEvent.OnClickCloseBottomSheet -> closeBottomSheet()
            is HomeViewEvent.OnClickCloseDialog -> closeDialog()
            is HomeViewEvent.OnChangeMemoDate -> setState { copy(selectedDate = event.date) }
            is HomeViewEvent.OnCreateMemo -> saveMemo(event.title, event.content)
            is HomeViewEvent.OnChangeTodoDate -> setState { copy(selectedDate = event.date) }
            is HomeViewEvent.OnCreateTodo -> saveTodo(event.title)
            is HomeViewEvent.OnClickMoveTodosToToday -> openDialog(HomeDialogTag.MoveTodos)
            is HomeViewEvent.OnConfirmMoveTodosToToday -> handleConfirmMoveTodosToToday()
        }
    }

    private fun handleChangeSelectDate(event: HomeViewEvent.OnChangeSelectDate) {
        setState { copy(selectedDate = event.date) }
        loadTasks(event.date)
    }

    private fun handleToggleTodo(
        event: HomeViewEvent.OnToggleTodo
    ) = viewModelScope.launch {
        try {
            toggleTodoUseCase(event.todoId)
            // 상태 갱신을 위해 현재 선택된 날짜의 데이터를 다시 로드
            loadTasks(state.selectedDate)
        } catch (e: Exception) {
            postSideEffect(HomeSideEffect.ShowSnackbar("할 일 상태 변경에 실패했습니다."))
        }
    }

    private fun handleDeleteTask(
        event: HomeViewEvent.OnDeleteTask
    ) = viewModelScope.launch {
        try {
            deleteTaskUseCase(event.taskId)
            postSideEffect(HomeSideEffect.ShowSnackbar("항목이 삭제되었습니다."))
        } catch (e: Exception) {
            postSideEffect(HomeSideEffect.ShowSnackbar("삭제에 실패했습니다."))
        }
    }

    private fun saveTodo(title: String) {
        if (title.isBlank()) {
            postSideEffect(HomeSideEffect.ShowSnackbar("제목을 입력해주세요."))
            return
        }

        viewModelScope.launch {
            setState { copy(isLoading = true) }
            try {
                val todo = Task.Todo(
                    title = title,
                    date = state.selectedDate,
                    isCompleted = false
                )
                addTodoUseCase(todo)
                closeBottomSheet()
            } catch (e: Exception) {
                postSideEffect(HomeSideEffect.ShowSnackbar("저장에 실패했습니다."))
            } finally {
                setState { copy(isLoading = false) }
            }
        }
    }

    private fun saveMemo(title: String, content: String) {
        if (title.isBlank()) {
            postSideEffect(HomeSideEffect.ShowSnackbar("제목을 입력해주세요."))
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
                postSideEffect(HomeSideEffect.ShowSnackbar("메모 저장에 실패했습니다."))
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

    private fun handleConfirmMoveTodosToToday() {
        closeDialog()
        moveTodosToToday()
    }

    private fun moveTodosToToday() = viewModelScope.launch {
        setState { copy(isLoading = true) }
        try {
            // 현재 선택된 날짜의 미완료 할 일들만 가져옴
            val uncompletedTodos = state.tasks
                .filterIsInstance<Task.Todo>()
                .filter { !it.isCompleted }

            // 오늘 날짜로 TD 옮기기
            uncompletedTodos.forEach { todo ->
                val updatedTodo = todo.copy(date = LocalDate.now())
                updateTodoUseCase(updatedTodo)
            }

            // 성공 메시지
            postSideEffect(HomeSideEffect.ShowSnackbar("미완료 할 일을 오늘로 이동했습니다."))
            // 오늘 날짜로 변경
            onEvent(HomeViewEvent.OnChangeTodoDate(LocalDate.now()))
        } catch (e: Exception) {
            postSideEffect(HomeSideEffect.ShowSnackbar("이동에 실패했습니다."))
        } finally {
            setState { copy(isLoading = false) }
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

    private fun openDialog(tag: HomeDialogTag) {
        setState { copy(dialogState = dialogState.open(tag)) }
    }

    private fun closeDialog() {
        setState { copy(dialogState = dialogState.close()) }
    }
}