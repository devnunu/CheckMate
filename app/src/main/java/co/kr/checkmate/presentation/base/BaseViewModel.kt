package co.kr.checkmate.presentation.base

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

abstract class BaseViewModel<S : Any, E : Any>(initialState: S) : ViewModel() {

    // 상태 관리
    private val _state = MutableStateFlow(initialState)
    val state: StateFlow<S> = _state.asStateFlow()

    // 사이드 이펙트 관리
    private val _sideEffect = Channel<E>()
    val sideEffect = _sideEffect.receiveAsFlow()

    // 상태 업데이트 함수
    protected fun setState(reducer: S.() -> S) {
        val newState = state.value.reducer()
        _state.value = newState
    }

    // 사이드 이펙트 발행 함수
    protected fun postSideEffect(effect: E) {
        viewModelScope.launch {
            _sideEffect.send(effect)
        }
    }
}