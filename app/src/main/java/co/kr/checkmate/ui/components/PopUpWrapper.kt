package co.kr.checkmate.ui.components

import androidx.compose.runtime.Composable

@Composable
fun <T> PopUpWrapper(
    dialogState: ModalState<T>,
    content: @Composable (tag: T) -> Unit
) {
    if (dialogState is ModalState.Opened) {
        content(dialogState.tag)
    }
}