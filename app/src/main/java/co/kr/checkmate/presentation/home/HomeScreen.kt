package co.kr.checkmate.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import co.kr.checkmate.domain.model.Task
import co.kr.checkmate.presentation.calendar.MonthCalendarScreen
import co.kr.checkmate.presentation.home.components.TaskPager
import kotlinx.coroutines.flow.Flow
import org.threeten.bp.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeState,
    onEvent: (HomeViewEvent) -> Unit,
    sideEffect: Flow<HomeSideEffect>
) {
    val snackbarHostState = remember { SnackbarHostState() }

    // 사이드 이펙트 처리
    LaunchedEffect(key1 = true) {
        sideEffect.collect { sideEffect ->
            when (sideEffect) {
                is HomeSideEffect.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(sideEffect.message)
                }
                // 추후 내비게이션 처리
                is HomeSideEffect.NavigateToAddTodo -> { }
                is HomeSideEffect.NavigateToAddMemo -> { }
            }
        }
    }

    AnimatedContent(
        targetState = state.showMonthCalendar,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "calendarScreenTransition"
    ) { showMonthCalendar ->
        if (showMonthCalendar) {
            // 월간 캘린더 화면
            MonthCalendarScreen(
                onBackPressed = {
                    onEvent(HomeViewEvent.ToggleMonthCalendar)
                },
                onDateSelected = { date ->
                    onEvent(HomeViewEvent.SelectDate(date))
                    onEvent(HomeViewEvent.ToggleMonthCalendar)
                },
                initialDate = state.selectedDate
            )
        } else {
            // 홈 화면
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = { Text("CheckMate") },
                        actions = {
                            // 캘린더 아이콘 추가
                            IconButton(onClick = { onEvent(HomeViewEvent.ToggleMonthCalendar) }) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "월간 캘린더 보기"
                                )
                            }
                        }
                    )
                },
                floatingActionButton = {
                    ExpandableFab(
                        isExpanded = state.isFabExpanded,
                        onExpandChange = { expanded ->
                            if (expanded) {
                                onEvent(HomeViewEvent.ExpandFab)
                            } else {
                                onEvent(HomeViewEvent.CollapseFab)
                            }
                        },
                        onAddTodo = {
                            onEvent(HomeViewEvent.NavigateToAddTodo(state.selectedDate))
                        },
                        onAddMemo = {
                            onEvent(HomeViewEvent.NavigateToAddMemo(state.selectedDate))
                        }
                    )
                },
                snackbarHost = { SnackbarHost(snackbarHostState) }
            ) { paddingValues ->
                if (state.isLoading && state.tasks.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    ) {
                        // 단순 날짜 텍스트만 표시
                        Text(
                            text = state.selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 (E)")),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )

                        // 태스크 페이저 - 간격 축소
                        TaskPager(
                            modifier = Modifier
                                .fillMaxWidth(),
                            initialDate = state.selectedDate,
                            tasks = state.tasks,
                            onDateChanged = { date ->
                                onEvent(HomeViewEvent.SelectDate(date))
                            },
                            onToggleTodo = { todoId ->
                                onEvent(HomeViewEvent.ToggleTodo(todoId))
                            },
                            onDeleteTask = { taskId ->
                                onEvent(HomeViewEvent.DeleteTask(taskId))
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TodoItem(
    todo: Task.Todo,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = todo.isCompleted,
                onCheckedChange = { onToggle() }
            )
            Text(
                text = todo.title,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
                textDecoration = if (todo.isCompleted) TextDecoration.LineThrough else null,
                color = if (todo.isCompleted)
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                else
                    MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "투두",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun MemoItem(
    memo: Task.Memo,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = memo.title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "메모",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = memo.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ExpandableFab(
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onAddTodo: () -> Unit,
    onAddMemo: () -> Unit
) {
    Box(
        contentAlignment = Alignment.BottomEnd
    ) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 72.dp, end = 4.dp)
            ) {
                FabMenuItem(
                    icon = Icons.Default.Check,
                    label = "투두",
                    onClick = {
                        onAddTodo()
                        onExpandChange(false)
                    }
                )

                FabMenuItem(
                    icon = Icons.Default.Edit,
                    label = "메모",
                    onClick = {
                        onAddMemo()
                        onExpandChange(false)
                    }
                )
            }
        }

        // 메인 FAB
        FloatingActionButton(
            onClick = { onExpandChange(!isExpanded) }
        ) {
            val rotation by animateFloatAsState(
                targetValue = if (isExpanded) 45f else 0f,
                label = "fabRotation"
            )
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (isExpanded) "접기" else "펼치기",
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

@Composable
fun FabMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(end = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            modifier = Modifier
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.medium
                )
                .padding(horizontal = 12.dp, vertical = 6.dp)
        )

        Spacer(modifier = Modifier.size(8.dp))

        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = MaterialTheme.colorScheme.primary
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }
    }
}

@Composable
fun Checkbox(
    checked: Boolean,
    onCheckedChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .background(
                color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
                shape = MaterialTheme.shapes.small
            )
            .clickable { onCheckedChange() }
            .border(
                width = 2.dp,
                color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = MaterialTheme.shapes.small
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "완료됨",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}