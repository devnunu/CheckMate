import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxState
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import co.kr.checkmate.domain.model.Task
import co.kr.checkmate.presentation.home.components.task.Checkbox
import co.kr.checkmate.ui.ext.clickableRipple

@Composable
fun TodoItem(
    modifier: Modifier = Modifier,
    todo: Task.Todo,
    onToggle: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onEdit: (Task.Todo) -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        initialValue = SwipeToDismissBoxValue.Settled,
        positionalThreshold = { it * 0.4f },
        confirmValueChange = { value ->
            when (value) {
                SwipeToDismissBoxValue.StartToEnd -> {
                    // 왼쪽에서 오른쪽으로 스와이프: 편집
                    onEdit(todo)
                    false // 자동으로 원위치로 돌아가도록 false 반환
                }
                SwipeToDismissBoxValue.EndToStart -> {
                    // 오른쪽에서 왼쪽으로 스와이프: 삭제
                    onDelete(todo.id)
                    false // 자동으로 원위치로 돌아가도록 false 반환
                }
                SwipeToDismissBoxValue.Settled -> true
            }
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,  // 왼쪽에서 오른쪽으로 스와이프 활성화
        enableDismissFromEndToStart = true,  // 오른쪽에서 왼쪽으로 스와이프 활성화
        backgroundContent = {
            SwipeBackground(dismissState)
        },
        content = {
            TodoContent(
                modifier = modifier,
                todo = todo,
                onToggle = onToggle
            )
        }
    )
}

@Composable
fun SwipeBackground(state: SwipeToDismissBoxState) {
    val direction = state.dismissDirection

    val color = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> MaterialTheme.colorScheme.primary // 편집 배경색
        SwipeToDismissBoxValue.EndToStart -> MaterialTheme.colorScheme.error   // 삭제 배경색
        SwipeToDismissBoxValue.Settled -> Color.Transparent
    }

    val alignment = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Alignment.CenterStart
        SwipeToDismissBoxValue.EndToStart -> Alignment.CenterEnd
        SwipeToDismissBoxValue.Settled -> Alignment.Center
    }

    val icon = when (direction) {
        SwipeToDismissBoxValue.StartToEnd -> Icons.Default.Edit
        SwipeToDismissBoxValue.EndToStart -> Icons.Default.Delete
        SwipeToDismissBoxValue.Settled -> null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(color)
            .padding(horizontal = 20.dp),
        contentAlignment = alignment
    ) {
        icon?.let {
            Icon(
                imageVector = it,
                contentDescription =
                    if (direction == SwipeToDismissBoxValue.StartToEnd) "편집" else "삭제",
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun TodoContent(
    modifier: Modifier = Modifier,
    todo: Task.Todo,
    onToggle: (Long) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickableRipple(bounded = true) { onToggle(todo.id) }
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 기존 TodoContent 내용 유지
        Checkbox(
            checked = todo.isCompleted,
            onCheckedChange = { onToggle(todo.id) }
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