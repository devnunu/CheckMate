package co.kr.checkmate.presentation.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import co.kr.checkmate.domain.model.Task
import co.kr.checkmate.presentation.home.calendar.MonthCalendar
import co.kr.checkmate.presentation.home.calendar.WeekCalendar
import kotlinx.coroutines.flow.Flow
import org.threeten.bp.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeState,
    onEvent: (HomeViewEvent) -> Unit,
    sideEffect: Flow<HomeSideEffect>,
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val listState = rememberLazyListState()

    // 스크롤 위치에 따라 캘린더 타입 변경
    val isScrolled by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 0 }
    }

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

    // 스크롤에 따른 캘린더 타입 변경
    LaunchedEffect(isScrolled) {
        if (isScrolled && state.calendarType == CalendarType.MONTH) {
            onEvent(HomeViewEvent.ChangeCalendarType)
        } else if (!isScrolled && state.calendarType == CalendarType.WEEK) {
            onEvent(HomeViewEvent.ChangeCalendarType)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CheckMate") }
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
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                state = listState
            ) {
                item {
                    // 캘린더 뷰
                    when (state.calendarType) {
                        CalendarType.MONTH -> {
                            MonthCalendar(
                                selectedDate = state.selectedDate,
                                onDateSelected = { date ->
                                    onEvent(HomeViewEvent.SelectDate(date))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                        }
                        CalendarType.WEEK -> {
                            WeekCalendar(
                                selectedDate = state.selectedDate,
                                onDateSelected = { date ->
                                    onEvent(HomeViewEvent.SelectDate(date))
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp)
                            )
                        }
                    }

                    // 선택된 날짜 헤더
                    Text(
                        text = "${state.selectedDate.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))} 항목",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }

                // 할 일 목록
                if (state.tasks.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "등록된 항목이 없습니다.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                } else {
                    items(state.tasks) { task ->
                        when (task) {
                            is Task.Todo -> {
                                TodoItem(
                                    todo = task,
                                    onToggle = { onEvent(HomeViewEvent.ToggleTodo(task.id)) },
                                    onDelete = { onEvent(HomeViewEvent.DeleteTask(task.id)) }
                                )
                            }
                            is Task.Memo -> {
                                MemoItem(
                                    memo = task,
                                    onDelete = { onEvent(HomeViewEvent.DeleteTask(task.id)) }
                                )
                            }
                        }
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