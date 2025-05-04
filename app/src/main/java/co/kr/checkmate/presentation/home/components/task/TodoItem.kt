package co.kr.checkmate.presentation.home.components.task

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.kr.checkmate.domain.model.Task
import co.kr.checkmate.ui.ext.clickableRipple
import org.threeten.bp.LocalDate

@Composable
fun TodoItem(
    modifier: Modifier = Modifier,
    todo: Task.Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onLongClick: () -> Unit,
) {
    // 완료 여부에 관계없이 항상 표시
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickableRipple(bounded = true) {
                onToggle()
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { onLongClick() }
                )
            }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = todo.isCompleted,
            onCheckedChange = { onToggle() }
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = todo.title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
            textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
            color = if (todo.isCompleted)
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            else
                MaterialTheme.colorScheme.onSurface
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun TodoItemPreview() {
    TodoItem(
        todo = Task.Todo(
            id = 1,
            title = "Todo Item",
            date = LocalDate.of(2023, 1, 1),
            isCompleted = false
        ),
        onToggle = {},
        onDelete = {},
        onLongClick = {}
    )
}