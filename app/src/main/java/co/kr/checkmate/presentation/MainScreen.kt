package co.kr.checkmate.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import co.kr.checkmate.presentation.home.HomeScreen
import co.kr.checkmate.presentation.home.HomeSideEffect
import co.kr.checkmate.presentation.home.HomeViewModel
import co.kr.checkmate.presentation.memo.MemoBottomSheet
import co.kr.checkmate.presentation.memo.MemoViewModel
import co.kr.checkmate.presentation.todo.TodoBottomSheet
import co.kr.checkmate.presentation.todo.TodoViewModel
import org.koin.androidx.compose.koinViewModel
import org.threeten.bp.LocalDate

@Composable
fun MainScreen() {
    val homeViewModel: HomeViewModel = koinViewModel()
    val todoViewModel: TodoViewModel = koinViewModel()
    val memoViewModel: MemoViewModel = koinViewModel()

    val homeState by homeViewModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    // 바텀시트 상태 관리
    var showTodoBottomSheet by remember { mutableStateOf(false) }
    var showMemoBottomSheet by remember { mutableStateOf(false) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    // 홈 화면 사이드 이펙트 처리
    LaunchedEffect(key1 = true) {
        homeViewModel.sideEffect.collect { effect ->
            when (effect) {
                is HomeSideEffect.NavigateToAddTodo -> {
                    selectedDate = effect.date
                    showTodoBottomSheet = true
                }
                is HomeSideEffect.NavigateToAddMemo -> {
                    selectedDate = effect.date
                    showMemoBottomSheet = true
                }
                else -> {}
            }
        }
    }

    // 홈 화면
    HomeScreen(
        state = homeState,
        onEvent = homeViewModel::onEvent,
        sideEffect = homeViewModel.sideEffect
    )

    // 투두 추가 바텀시트
    if (showTodoBottomSheet) {
        TodoBottomSheet(
            onDismiss = {
                showTodoBottomSheet = false
            },
            initialDate = selectedDate,
            viewModel = todoViewModel,
            sideEffect = todoViewModel.sideEffect
        )
    }

    // 메모 추가 바텀시트
    if (showMemoBottomSheet) {
        MemoBottomSheet(
            onDismiss = {
                showMemoBottomSheet = false
            },
            initialDate = selectedDate,
            viewModel = memoViewModel,
            sideEffect = memoViewModel.sideEffect
        )
    }
}