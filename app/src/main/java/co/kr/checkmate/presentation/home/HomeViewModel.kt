package co.kr.checkmate.presentation.home

import androidx.lifecycle.viewModelScope
import co.kr.checkmate.domain.model.Task
import co.kr.checkmate.domain.usecase.AddMemoUseCase
import co.kr.checkmate.domain.usecase.AddTodoUseCase
import co.kr.checkmate.domain.usecase.DeleteTaskUseCase
import co.kr.checkmate.domain.usecase.GetWeekTasksUseCase
import co.kr.checkmate.domain.usecase.ToggleTodoUseCase
import co.kr.checkmate.domain.usecase.UpdateTodoUseCase
import co.kr.checkmate.ui.base.BaseViewModel
import kotlinx.coroutines.launch
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate

class HomeViewModel(
    private val getWeekTasksUseCase: GetWeekTasksUseCase,
    private val toggleTodoUseCase: ToggleTodoUseCase,
    private val deleteTaskUseCase: DeleteTaskUseCase,
    private val addMemoUseCase: AddMemoUseCase,
    private val addTodoUseCase: AddTodoUseCase,
    private val updateTodoUseCase: UpdateTodoUseCase
) : BaseViewModel<HomeState, HomeViewEvent, HomeSideEffect>(
    initialState = HomeState()
) {

    init {
        loadWeekTasks(LocalDate.now())
    }

    override fun onEvent(event: HomeViewEvent) {
        when (event) {
            is HomeViewEvent.OnChangeSelectDate -> handleChangeSelectDate(event)
            is HomeViewEvent.OnToggleTodo -> handleToggleTodo(event)
            is HomeViewEvent.OnSwipeDeleteTodo -> handleDeleteTask(event)
            is HomeViewEvent.OnClickCalendarIcon -> postSideEffect(HomeSideEffect.NavigateToCalendar)
            is HomeViewEvent.OnExpandFab -> setState { copy(isFabExpanded = true) }
            is HomeViewEvent.OnCollapseFab -> setState { copy(isFabExpanded = false) }
            is HomeViewEvent.OnClickAddTodoBtn -> handleClickAddTodoBtn()
            is HomeViewEvent.OnClickAddMemoBtn -> openBottomSheet(HomeBottomSheetTag.Memo)
            is HomeViewEvent.OnClickCloseBottomSheet -> closeBottomSheet()
            is HomeViewEvent.OnClickCloseDialog -> closeDialog()
            is HomeViewEvent.OnCreateMemo -> saveMemo(event.title, event.content, event.date)
            is HomeViewEvent.OnCreateTodo -> saveTodo(event.title, event.date)
            is HomeViewEvent.OnSwipeEditTodo -> handleSwipeEditTodo(event)
            is HomeViewEvent.OnUpdateTodo -> updateTodo(event.todoId, event.title)
            is HomeViewEvent.OnClickDeleteTodo -> handleClickDeleteTodo(event.todoId)
            is HomeViewEvent.OnClickMoveTodosToToday -> openDialog(HomeDialogTag.MoveTodos)
            is HomeViewEvent.OnConfirmMoveTodosToToday -> handleConfirmMoveTodosToToday()
        }
    }

    private fun handleChangeSelectDate(event: HomeViewEvent.OnChangeSelectDate) {
        setState { copy(selectedDate = event.date) }
        // 이전 주나 다음 주로 이동할 경우에만 새로 로드
        val currentMonday = state.selectedDate.with(DayOfWeek.MONDAY)
        val newMonday = event.date.with(DayOfWeek.MONDAY)
        if (!currentMonday.isEqual(newMonday)) {
            loadWeekTasks(event.date)
        }
    }

    private fun handleToggleTodo(
        event: HomeViewEvent.OnToggleTodo
    ) = viewModelScope.launch {
        try {
            toggleTodoUseCase(event.todoId)
            // 상태 갱신을 위해 현재 선택된 날짜의 데이터를 다시 로드
            loadWeekTasks(state.selectedDate)
        } catch (e: Exception) {
            postSideEffect(HomeSideEffect.ShowSnackbar("할 일 상태 변경에 실패했습니다."))
        }
    }

    private fun handleDeleteTask(
        event: HomeViewEvent.OnSwipeDeleteTodo
    ) = viewModelScope.launch {
        try {
            deleteTaskUseCase(event.taskId)
            loadWeekTasks(state.selectedDate)
            postSideEffect(HomeSideEffect.ShowSnackbar("항목이 삭제되었습니다."))
        } catch (e: Exception) {
            postSideEffect(HomeSideEffect.ShowSnackbar("삭제에 실패했습니다."))
        }
    }

    private fun handleClickAddTodoBtn() {
        openBottomSheet(HomeBottomSheetTag.Todo(isEditMode = false))
    }

    private fun handleSwipeEditTodo(event: HomeViewEvent.OnSwipeEditTodo) {
        openBottomSheet(HomeBottomSheetTag.Todo(isEditMode = true, selectedTodo = event.todo))
    }

    private fun handleClickDeleteTodo(todoId: Long) {
        handleDeleteTask(HomeViewEvent.OnSwipeDeleteTodo(todoId))
        closeBottomSheet()
    }

    private fun updateTodo(todoId: Long, title: String) = viewModelScope.launch {
        if (title.isBlank()) {
            postSideEffect(HomeSideEffect.ShowSnackbar("제목을 입력해주세요."))
            return@launch
        }
        setState { copy(isLoading = true) }
        try {
            // 모든 태스크에서 해당 ID의 TD 찾기
            val allTasks = state.weekTasks.values.flatten()
            val todoToUpdate = allTasks.filterIsInstance<Task.Todo>().find { it.id == todoId }

            todoToUpdate?.let { todo ->
                val updatedTodo = todo.copy(title = title, date = state.selectedDate)
                updateTodoUseCase(updatedTodo)

                // 상태 갱신을 위해 태스크 다시 로드
                loadWeekTasks(state.selectedDate)

                closeBottomSheet()
                postSideEffect(HomeSideEffect.ShowSnackbar("할 일이 수정되었습니다."))
            }
        } catch (e: Exception) {
            postSideEffect(HomeSideEffect.ShowSnackbar("수정에 실패했습니다."))
        } finally {
            setState { copy(isLoading = false) }
        }
    }

    private fun saveTodo(title: String, date: LocalDate?) = viewModelScope.launch {
        if (title.isBlank()) {
            postSideEffect(HomeSideEffect.ShowSnackbar("제목을 입력해주세요."))
            return@launch
        }
        setState { copy(isLoading = true) }
        try {
            val taskDate = date ?: state.selectedDate
            val todo = Task.Todo(
                title = title,
                date = taskDate,
                isCompleted = false
            )
            addTodoUseCase(todo)
            closeBottomSheet()

            // 새 태스크가 추가된 주의 데이터 다시 로드
            loadWeekTasks(taskDate)
        } catch (e: Exception) {
            postSideEffect(HomeSideEffect.ShowSnackbar("저장에 실패했습니다."))
        } finally {
            setState { copy(isLoading = false) }
        }
    }

    private fun saveMemo(
        title: String,
        content: String,
        date: LocalDate?
    ) = viewModelScope.launch {
        if (title.isBlank()) {
            postSideEffect(HomeSideEffect.ShowSnackbar("제목을 입력해주세요."))
            return@launch
        }
        setState { copy(isLoading = true) }
        try {
            val taskDate = date ?: state.selectedDate
            val memo = Task.Memo(
                title = title,
                content = content,
                date = taskDate
            )
            addMemoUseCase(memo)
            closeBottomSheet()

            // 새 메모가 추가된 주의 데이터 다시 로드
            loadWeekTasks(taskDate)
        } catch (e: Exception) {
            postSideEffect(HomeSideEffect.ShowSnackbar("메모 저장에 실패했습니다."))
        } finally {
            setState { copy(isLoading = false) }
        }
    }

    private fun loadWeekTasks(date: LocalDate) {
        viewModelScope.launch {
            setState { copy(isLoading = true) }
            try {
                // 날짜가 속한 주의 월요일과 일요일 계산
                val monday = date.with(DayOfWeek.MONDAY)
                val sunday = date.with(DayOfWeek.SUNDAY)

                getWeekTasksUseCase(monday, sunday).collect { weekTasks ->
                    setState {
                        copy(
                            weekTasks = weekTasks,
                            isLoading = false
                        )
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
            val uncompletedTodos = getUncompletedTodos()

            // 오늘 날짜로 TD 옮기기
            uncompletedTodos.forEach { todo ->
                val updatedTodo = todo.copy(date = LocalDate.now())
                updateTodoUseCase(updatedTodo)
            }

            // 성공 메시지
            postSideEffect(HomeSideEffect.ShowSnackbar("미완료 할 일을 오늘로 이동했습니다."))
            // 오늘 날짜로 변경
            setState { copy(selectedDate = LocalDate.now()) }
        } catch (e: Exception) {
            postSideEffect(HomeSideEffect.ShowSnackbar("이동에 실패했습니다."))
        } finally {
            setState { copy(isLoading = false) }
        }
    }

    private fun getUncompletedTodos(): List<Task.Todo> =
        state.tasks
            .filterIsInstance<Task.Todo>()
            .filter { !it.isCompleted }

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