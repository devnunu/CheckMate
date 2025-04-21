package co.kr.checkmate.ui.ext

import android.annotation.SuppressLint
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import co.kr.checkmate.ui.base.BaseViewModel
import co.kr.checkmate.ui.base.SideEffect
import co.kr.checkmate.ui.base.ViewEvent
import co.kr.checkmate.ui.base.ViewState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull

@SuppressLint("ComposableNaming")
@Composable
fun <STATE : ViewState,
        VIEW_EVENT : ViewEvent,
        SIDE_EFFECT : SideEffect>
        BaseViewModel<STATE, VIEW_EVENT, SIDE_EFFECT>.collectSideEffect(
    lifecycleState: Lifecycle.State = Lifecycle.State.STARTED,
    lifecycleOwner: LifecycleOwner = LocalLifecycleOwner.current,
    sideEffect: (suspend (sideEffect: SIDE_EFFECT) -> Unit)? = null
) {
    val sideEffectFlow: Flow<SIDE_EFFECT?> = this.sideEffect

    val callback by rememberUpdatedState(newValue = sideEffect)

    LaunchedEffect(sideEffectFlow, lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(lifecycleState) {
            sideEffectFlow.mapNotNull { it }.collect {
                callback?.invoke(it)
            }
        }
    }
}

// popBackStack 시에 recomposable로 인해 LaunchedEffect가 한번더 동작하는것을 방지하기 위해 사용
// navigate로 페이지에 다시 진입하면 정상적으로 계속 호출됨, popBackStack로 복귀했을때만 동작하지 않도록 처리되는것을 확인
@Composable
fun LaunchedEffectOnce(
    content: suspend CoroutineScope.() -> Unit
) {
    var hasLaunched by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!hasLaunched) {
            content()
            hasLaunched = true
        }
    }
}
