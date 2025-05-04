package co.kr.checkmate.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import co.kr.checkmate.presentation.home.components.bottomsheet.MemoBottomSheet
import co.kr.checkmate.presentation.home.components.bottomsheet.TodoBottomSheet
import co.kr.checkmate.presentation.home.components.date.TopDateSection
import co.kr.checkmate.presentation.home.components.fab.ExpandableFab
import co.kr.checkmate.presentation.home.components.pager.PageIndicator
import co.kr.checkmate.presentation.home.components.task.TaskList
import co.kr.checkmate.ui.components.BottomSheetWrapper
import co.kr.checkmate.ui.components.PopUpWrapper
import co.kr.checkmate.ui.ext.collectSideEffect
import co.kr.checkmate.ui.navigation.NavRoute
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate

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
        todayDate = LocalDate.now(),
        snackBarHostState = snackBarHostState
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    state: HomeState,
    onEvent: (HomeViewEvent) -> Unit,
    todayDate: LocalDate,
    snackBarHostState: SnackbarHostState
) {
    // 현재 날짜의 해당 주 월요일 구하기
    var currentWeekMonday by remember {
        mutableStateOf(state.selectedDate.with(DayOfWeek.MONDAY))
    }

    // 현재 주의 모든 날짜 (월~일)
    val weekDates = remember(currentWeekMonday) {
        (0..6).map { currentWeekMonday.plusDays(it.toLong()) }
    }

    // 표시할 날짜와 가장 가까운 인덱스 찾기
    val initialPageIndex = remember(weekDates, state.selectedDate) {
        weekDates.indexOfFirst { it.isEqual(state.selectedDate) }.takeIf { it >= 0 } ?: 0
    }

    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { 7 }
    )

    // 페이지 변경 시 호출
    LaunchedEffect(pagerState.currentPage) {
        val newDate = weekDates[pagerState.currentPage]
        onEvent(HomeViewEvent.OnChangeSelectDate(newDate))
    }

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

    PopUpWrapper(
        dialogState = state.dialogState
    ) { tag ->
        when (tag) {
            is HomeDialogTag.MoveTodos -> {
                AlertDialog(
                    onDismissRequest = {
                        onEvent(HomeViewEvent.OnClickCloseBottomSheet) // 기존 닫기 이벤트 재활용
                    },
                    title = { Text("할 일 이동") },
                    text = { Text("체크되지 않은 TODO를 오늘날짜로 옮기시겠습니까?") },
                    confirmButton = {
                        TextButton(
                            onClick = { onEvent(HomeViewEvent.OnConfirmMoveTodosToToday) }
                        ) {
                            Text("네")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = {
                                onEvent(HomeViewEvent.OnClickCloseDialog) // 기존 닫기 이벤트 재활용
                            }
                        ) {
                            Text("아니오")
                        }
                    }
                )
            }
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
                TopDateSection(
                    pagerState = pagerState,
                    selectedDate = state.selectedDate,
                    todayDate = todayDate,
                    currentWeekMonday = currentWeekMonday,
                    onClickMoveTodosToToday = { onEvent(HomeViewEvent.OnClickMoveTodosToToday) },
                    onDateChanged = { date ->
                        onEvent(HomeViewEvent.OnChangeSelectDate(date))
                    },
                    onUpdateCurrentWeekMonday = { newMonday ->
                        currentWeekMonday = newMonday
                    }
                )
                PageIndicator(
                    pagerState = pagerState,
                    weekDates = weekDates
                )
                Box(modifier = modifier.weight(1f)) {
                    val tasks = state.tasks
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val pageDate = weekDates[page]
                        val dayTasks = remember(tasks, pageDate) {
                            tasks.filter { it.date == pageDate }
                        }
                        TaskList(
                            modifier = Modifier.fillMaxSize(),
                            tasks = dayTasks,
                            onToggleTodo = { todoId -> onEvent(HomeViewEvent.OnToggleTodo(todoId)) },
                            onDeleteTask = { taskId ->
                                onEvent(HomeViewEvent.OnDeleteTask(taskId))
                            }
                        )
                    }
                }
            }
            if (state.isLoading && state.tasks.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    val snackBarHostState = remember { SnackbarHostState() }
    HomeScreen(
        state = HomeState(
            selectedDate = LocalDate.of(2025, 1, 1)
        ),
        onEvent = {},
        todayDate = LocalDate.of(2025, 1, 1),
        snackBarHostState = snackBarHostState,
    )
}