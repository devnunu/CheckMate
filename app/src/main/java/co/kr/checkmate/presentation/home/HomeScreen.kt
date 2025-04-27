package co.kr.checkmate.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import co.kr.checkmate.presentation.home.components.bottomsheet.MemoBottomSheet
import co.kr.checkmate.presentation.home.components.bottomsheet.TodoBottomSheet
import co.kr.checkmate.presentation.home.components.fab.ExpandableFab
import co.kr.checkmate.presentation.home.components.task.TaskPager
import co.kr.checkmate.ui.components.BottomSheetWrapper
import co.kr.checkmate.ui.ext.collectSideEffect
import co.kr.checkmate.ui.navigation.NavRoute
import co.kr.checkmate.ui.theme.blue10
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: HomeViewModel,
    navController: NavController
) {
    val snackBarHostState = remember { SnackbarHostState() }
    viewModel.collectSideEffect { sideEffect ->
        when (sideEffect) {
            is HomeSideEffect.ShowSnackbar -> {
                snackBarHostState.showSnackbar(sideEffect.message)
            }

            is HomeSideEffect.NavigateToCalendar -> {
                navController.navigate(NavRoute.Calendar)
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
    BottomSheetWrapper(
        viewModelSheetState = state.bottomSheetState,
        onCloseBottomSheet = { onEvent(HomeViewEvent.OnClickCloseBottomSheet) }
    ) { tag ->
        when (tag) {
            is HomeBottomSheetTag.Todo -> {
                TodoBottomSheet(
                    state = state,
                    onEvent = onEvent
                )
            }

            is HomeBottomSheetTag.Memo -> {
                MemoBottomSheet(
                    state = state,
                    onEvent = onEvent
                )
            }

            else -> Unit
        }
    }

    // 홈 화면
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CheckMate") },
                actions = {
                    // 캘린더 아이콘 추가
                    IconButton(onClick = { onEvent(HomeViewEvent.OnClickCalendarIcon) }) {
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
                        onEvent(HomeViewEvent.OnExpandFab)
                    } else {
                        onEvent(HomeViewEvent.OnCollapseFab)
                    }
                },
                onAddTodo = {
                    onEvent(HomeViewEvent.OnClickAddTodoBtn)
                },
                onAddMemo = {
                    onEvent(HomeViewEvent.OnClickAddMemoBtn)
                }
            )
        },
        snackbarHost = { SnackbarHost(snackBarHostState) }
    ) { paddingValues ->

        Box(
            modifier = Modifier.fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // HomeScreen.kt의 날짜 표시 부분 수정
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val yearMonth = state.selectedDate.format(DateTimeFormatter.ofPattern("yyyy. MM월"))
                    Text(
                        text = yearMonth,
                        style = MaterialTheme.typography.labelLarge,
                        textAlign = TextAlign.Start,
                        color = blue10,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val date = state.selectedDate.format(DateTimeFormatter.ofPattern("dd일.E"))
                        Text(
                            text = date,
                            style = MaterialTheme.typography.headlineLarge,
                            textAlign = TextAlign.Start,
                            color = blue10,
                        )

                        // 오늘 날짜인 경우 "Today" 표시
                        if (state.selectedDate.isEqual(LocalDate.now())) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Today",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .background(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                    }
                }


                // 태스크 페이저 - 간격 축소
                TaskPager(
                    modifier = Modifier
                        .weight(1f),
                    initialDate = state.selectedDate,
                    tasks = state.tasks,
                    onDateChanged = { date ->
                        onEvent(HomeViewEvent.OnChangeSelectDate(date))
                    },
                    onToggleTodo = { todoId ->
                        onEvent(HomeViewEvent.OnToggleTodo(todoId))
                    },
                    onDeleteTask = { taskId ->
                        onEvent(HomeViewEvent.OnDeleteTask(taskId))
                    }
                )
            }
            if (state.isLoading && state.tasks.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}