package co.kr.checkmate.presentation.home.components.task

import TodoItem
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.kr.checkmate.domain.model.Task
import co.kr.checkmate.ui.theme.gray80
import org.threeten.bp.LocalDate

@Composable
fun TaskList(
    modifier: Modifier = Modifier,
    tasks: List<Task>,
    onToggleTodo: (Long) -> Unit,
    onDeleteTodo: (Long) -> Unit,
    onEditTodo: (Task.Todo) -> Unit
) {
    LazyColumn(
        modifier = modifier
    ) {
        // 여기서 아이템을 필터링하지 않고 모두 표시합니다.
        items(tasks.sortedByDescending { it.id }) { task ->
            when (task) {
                is Task.Todo -> {
                    TodoItem(
                        todo = task,
                        onToggle = { todoId -> onToggleTodo(todoId) },
                        onDelete = { todoId -> onDeleteTodo(todoId) },
                        onEdit = { todo -> onEditTodo(todo) },
                    )
                }

                is Task.Memo -> {
                    MemoItem(
                        memo = task,
                        onDelete = { onDeleteTodo(task.id) }
                    )
                }
            }
            Divider(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                color = gray80,
                thickness = 1.dp
            )
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
        onDeleteTodo = {},
        onEditTodo = {}
    )

}