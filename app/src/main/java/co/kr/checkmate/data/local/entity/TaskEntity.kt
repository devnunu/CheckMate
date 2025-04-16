package co.kr.checkmate.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import co.kr.checkmate.domain.model.Task
import org.threeten.bp.LocalDate

sealed class TaskEntity {
    @Entity(tableName = "todos")
    data class TodoEntity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val title: String,
        val date: Long, // LocalDate를 Long으로 저장
        val isCompleted: Boolean = false
    ) {
        fun toDomain(): Task.Todo = Task.Todo(
            id = id,
            title = title,
            date = LocalDate.ofEpochDay(date),
            isCompleted = isCompleted
        )
    }

    @Entity(tableName = "memos")
    data class MemoEntity(
        @PrimaryKey(autoGenerate = true) val id: Long = 0,
        val title: String,
        val content: String,
        val date: Long
    ) {
        fun toDomain(): Task.Memo = Task.Memo(
            id = id,
            title = title,
            content = content,
            date = LocalDate.ofEpochDay(date)
        )
    }
}

// 도메인 모델을 엔티티로 변환하는 확장 함수
fun Task.Todo.toEntity(): TaskEntity.TodoEntity = TaskEntity.TodoEntity(
    id = id,
    title = title,
    date = date.toEpochDay(),
    isCompleted = isCompleted
)

fun Task.Memo.toEntity(): TaskEntity.MemoEntity = TaskEntity.MemoEntity(
    id = id,
    title = title,
    content = content,
    date = date.toEpochDay()
)