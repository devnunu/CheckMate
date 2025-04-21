package co.kr.checkmate.ui.base

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update

abstract class BaseViewModel<STATE : ViewState, VIEW_EVENT : ViewEvent, SIDE_EFFECT : SideEffect>(
    initialState: STATE
) : ViewModel() {

    private val _stateFlow = MutableStateFlow(initialState)
    val stateFlow: StateFlow<STATE> = _stateFlow

    protected val state: STATE
        get() = _stateFlow.value

    private val _sideEffects = Channel<SIDE_EFFECT>(capacity = Channel.BUFFERED)
    val sideEffect: Flow<SIDE_EFFECT?> = _sideEffects.receiveAsFlow()

    fun setState(reducer: STATE.() -> STATE) {
        _stateFlow.update(reducer)
    }

    fun postSideEffect(sideEffect: SIDE_EFFECT) {
        _sideEffects.trySend(sideEffect)
    }

    open fun onEvent(event: VIEW_EVENT) {
        // 상속받아 사용하도록 처리
    }
}