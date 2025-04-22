package co.kr.checkmate.presentation.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.kr.checkmate.presentation.calendar.CalendarScreen
import co.kr.checkmate.presentation.home.components.bottomsheet.MemoBottomSheet
import co.kr.checkmate.presentation.home.components.bottomsheet.TodoBottomSheet
import co.kr.checkmate.presentation.home.components.fab.ExpandableFab
import co.kr.checkmate.presentation.home.components.task.TaskPager
import co.kr.checkmate.ui.components.BottomSheetWrapper
import co.kr.checkmate.ui.ext.collectSideEffect
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel
) {
    val snackBarHostState = remember { SnackbarHostState() }
    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is HomeSideEffect.ShowSnackbar -> {
                snackBarHostState.showSnackbar(sideEffect.message)
            }
        }
    }

    HomeScreen(
        modifier = modifier,
        state = viewModel.stateFlow.collectAsState().value,
        onEvent = viewModel::onEvent,
        snackBarHostState = snackBarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeState,
    onEvent: (HomeViewEvent) -> Unit,
    snackBarHostState: SnackbarHostState
) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    BottomSheetWrapper(
        viewModelSheetState = state.bottomSheetState,
        onCloseBottomSheet = { onEvent(HomeViewEvent.OnClickCloseBottomSheet) }
    ) { tag ->
        when (tag) {
            is HomeBottomSheetTag.Todo -> {
                selectedDate = tag.date
                TodoBottomSheet(
                    state = state,
                    onEvent = onEvent
                )
            }

            is HomeBottomSheetTag.Memo -> {
                selectedDate = tag.date
                MemoBottomSheet(
                    state = state,
                    onEvent = onEvent
                )
            }

            else -> Unit
        }
    }

    AnimatedContent(
        targetState = state.showMonthCalendar,
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "calendarScreenTransition"
    ) { showMonthCalendar ->
        if (showMonthCalendar) {
            // 월간 캘린더 화면
            CalendarScreen(
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
                snackbarHost = { SnackbarHost(snackBarHostState) }
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