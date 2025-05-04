package co.kr.checkmate.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavController
import co.kr.checkmate.presentation.home.components.bottomsheet.MemoBottomSheet
import co.kr.checkmate.presentation.home.components.bottomsheet.TodoBottomSheet
import co.kr.checkmate.presentation.home.components.date.TopDateSection
import co.kr.checkmate.presentation.home.components.fab.ExpandableFab
import co.kr.checkmate.presentation.home.components.pager.TaskPager
import co.kr.checkmate.ui.components.BottomSheetWrapper
import co.kr.checkmate.ui.components.PopUpWrapper
import co.kr.checkmate.ui.ext.collectSideEffect
import co.kr.checkmate.ui.navigation.NavRoute
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
                    state = state,
                    onEvent = onEvent
                )


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

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    val snackBarHostState = remember { SnackbarHostState() }
    HomeScreen(
        state = HomeState(
            selectedDate = LocalDate.of(2025, 1, 1)
        ),
        onEvent = {},
        snackBarHostState = snackBarHostState,
    )
}
