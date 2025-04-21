package co.kr.checkmate.presentation.todo

import androidx.lifecycle.viewModelScope
import co.kr.checkmate.domain.model.Task
import co.kr.checkmate.domain.usecase.AddTodoUseCase
import co.kr.checkmate.ui.base.BaseViewModel
import kotlinx.coroutines.launch

class TodoViewModel(
    private val addTodoUseCase: AddTodoUseCase
) : BaseViewModel<TodoState, TodoSideEffect>(TodoState()) {

    fun processEvent(event: TodoViewEvent) {
        when (event) {
            is TodoViewEvent.UpdateTitle -> {
                setState { copy(title = event.title) }
            }
            is TodoViewEvent.SetDate -> {
                setState { copy(date = event.date) }
            }
            is TodoViewEvent.SaveTodo -> saveTodo()
            is TodoViewEvent.Dismiss -> {
                postSideEffect(TodoSideEffect.Dismissed)
            }
        }
    }

    private fun saveTodo() {
        val currentState = state.value

        if (currentState.title.isBlank()) {
            postSideEffect(TodoSideEffect.ShowError("제목을 입력해주세요."))
            return
        }

        viewModelScope.launch {
            setState { copy(isLoading = true) }
            try {
                val todo = Task.Todo(
                    title = currentState.title,
                    date = currentState.date,
                    isCompleted = false
                )
                addTodoUseCase(todo)
                postSideEffect(TodoSideEffect.TodoSaved)
            } catch (e: Exception) {
                postSideEffect(TodoSideEffect.ShowError("저장에 실패했습니다."))
            } finally {
                setState { copy(isLoading = false) }
            }
        }
    }
}