package co.kr.checkmate.ui.ext

import android.annotation.SuppressLint
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
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

private const val DEFAULT_THROTTLE_DURATION = 300L

fun Modifier.clickableRipple(
    bounded: Boolean = false,
    throttleDuration: Long = DEFAULT_THROTTLE_DURATION,
    rippleColor: Color? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    var lastEventMilli by remember { mutableStateOf(0L) }
    val interactionSource = remember { MutableInteractionSource() }

    if (onLongClick != null) {
        this.indication(
                interactionSource = interactionSource,
                indication = ripple(bounded = bounded, color = rippleColor ?: Color.Unspecified)
            )
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        onLongClick()
                    },
                    onTap = {
                        val now = System.currentTimeMillis()
                        if (now - lastEventMilli >= throttleDuration) {
                            lastEventMilli = now
                            onClick()
                        }
                    },
                    onPress = {
                        // 눌렀을 때 ripple 효과를 위한 이벤트 발생
                        val press = PressInteraction.Press(it)
                        interactionSource.emit(press)

                        // 누르고 있는 동안 대기
                        tryAwaitRelease()

                        // 손을 뗐을 때 ripple 효과 종료
                        interactionSource.emit(PressInteraction.Release(press))
                    }
                )
            }
    } else {
        this.clickable(
            interactionSource = interactionSource,
            indication = ripple(bounded = bounded, color = rippleColor ?: Color.Unspecified),
            onClick = {
                val now = System.currentTimeMillis()
                if (now - lastEventMilli >= throttleDuration) {
                    lastEventMilli = now
                    onClick()
                }
            }
        )
    }
}

fun Modifier.clickableNonIndication(
    duration: Long = DEFAULT_THROTTLE_DURATION,
    interactionSource: MutableInteractionSource = MutableInteractionSource(),
    onClick: () -> Unit
): Modifier = composed {
    var lastEventMilli by remember { mutableStateOf(0L) }
    this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = {
            val now = System.currentTimeMillis()
            if (now - lastEventMilli >= duration) {
                lastEventMilli = now
                onClick()
            }
        }
    )
}
