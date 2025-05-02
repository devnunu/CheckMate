package co.kr.checkmate.presentation.home.components.task

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.kr.checkmate.domain.model.Task
import org.threeten.bp.LocalDate

@Composable
fun TaskList(
    modifier: Modifier = Modifier,
    tasks: List<Task>,
    onToggleTodo: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit,
) {
    LazyColumn(
        modifier = modifier
            .padding(top = 8.dp)
    ) {
        if (tasks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "등록된 항목이 없습니다",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // 여기서 아이템을 필터링하지 않고 모두 표시합니다.
            items(tasks.sortedByDescending { it.id }) { task ->
                when (task) {
                    is Task.Todo -> {
                        TodoItem(
                            todo = task,
                            onToggle = { onToggleTodo(task.id) },
                            onDelete = { onDeleteTask(task.id) }
                        )
                    }

                    is Task.Memo -> {
                        MemoItem(
                            memo = task,
                            onDelete = { onDeleteTask(task.id) }
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun TaskListPreview() {
    TaskList(
        tasks = listOf(
            Task.Todo(
                id = 1,
                title = "Todo Item",
                date = LocalDate.of(2023, 1, 1),
                isCompleted = false
            ),
            Task.Memo(
                id = 1,
                title = "메모 제목",
                content = "메모 내용입니다.",
                date = LocalDate.of(2023, 1, 1)
            )
        ),
        onToggleTodo = {},
        onDeleteTask = {},
    )

}