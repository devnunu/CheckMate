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
import androidx.compose.ui.unit.dp
import co.kr.checkmate.domain.model.Task
import org.threeten.bp.LocalDate

@Composable
fun TaskList(
    date: LocalDate,
    tasks: List<Task>,
    onToggleTodo: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
        } else {
            items(tasks) { task ->
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