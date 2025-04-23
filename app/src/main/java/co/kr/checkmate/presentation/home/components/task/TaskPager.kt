package co.kr.checkmate.presentation.home.components.task

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import co.kr.checkmate.domain.model.Task
import kotlinx.coroutines.launch
import org.threeten.bp.LocalDate

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TaskPager(
    initialDate: LocalDate,
    tasks: List<Task>,
    onDateChanged: (LocalDate) -> Unit,
    onToggleTodo: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // 항상 3페이지만 사용: 전날, 오늘, 다음날
    val pageCount = 3
    val middlePage = 1 // 가운데 페이지 인덱스 (0: 이전날, 1: 현재날, 2: 다음날)

    // 페이저 상태 초기화 - 항상 가운데 페이지(현재 날짜)에서 시작
    val pagerState = rememberPagerState(
        initialPage = middlePage,
        pageCount = { pageCount }
    )

    val coroutineScope = rememberCoroutineScope()

    // 각 페이지에 표시할 날짜 계산
    val pageDates = remember(initialDate) {
        listOf(
            initialDate.minusDays(1), // 이전 날짜
            initialDate,              // 현재 날짜
            initialDate.plusDays(1)   // 다음 날짜
        )
    }

    // 페이지가 변경되었을 때 (스와이프 후)
    LaunchedEffect(pagerState.currentPage, initialDate) {
        // 현재 페이지가 가운데가 아니면 (사용자가 스와이프 했다면)
        if (pagerState.currentPage != middlePage) {
            // 새로운 날짜 계산 (이전날 또는 다음날)
            val newDate = if (pagerState.currentPage == 0) {
                initialDate.minusDays(1)
            } else {
                initialDate.plusDays(1)
            }

            // 상위 컴포넌트에 날짜 변경 알림
            onDateChanged(newDate)

            // 페이저를 가운데 페이지로 즉시 재설정
            coroutineScope.launch {
                pagerState.scrollToPage(middlePage)
            }
        }
    }

    HorizontalPager(
        modifier = modifier,
        state = pagerState,
    ) { page ->
        // 현재 페이지에 해당하는 날짜
        val pageDate = pageDates[page]

        // 해당 날짜의 작업 필터링
        val dayTasks = remember(tasks, pageDate) {
            tasks.filter { it.date == pageDate }
        }

        // 태스크 목록 표시
        TaskList(
            modifier.fillMaxSize(),
            tasks = dayTasks,
            onToggleTodo = onToggleTodo,
            onDeleteTask = onDeleteTask
        )
    }
}