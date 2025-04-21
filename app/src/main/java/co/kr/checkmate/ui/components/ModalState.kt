package co.kr.checkmate.ui.components

sealed class ModalState<out T>(open val tag: T) {
    data class Opened<out T>(override val tag: T) : ModalState<T>(tag = tag)
    data class Closed<out T>(override val tag: T) : ModalState<T>(tag = tag)

    fun close() = Closed(tag)
    fun <T> open(tag: T) = Opened(tag)
}
