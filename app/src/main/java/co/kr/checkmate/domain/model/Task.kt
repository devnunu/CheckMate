package co.kr.checkmate.domain.model

import org.threeten.bp.LocalDate

sealed class Task {
    abstract val id: Long
    abstract val date: LocalDate

    data class Todo(
        override val id: Long = 0,
        val title: String,
        override val date: LocalDate,
        val isCompleted: Boolean = false
    ): Task()

    data class Memo(
        override val id: Long = 0,
        val title: String,
        val content: String,
        override val date: LocalDate
    ): Task()
}