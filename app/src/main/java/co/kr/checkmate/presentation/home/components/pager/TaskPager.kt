package co.kr.checkmate.presentation.home.components.pager

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import co.kr.checkmate.domain.model.Task
import co.kr.checkmate.presentation.home.components.task.TaskList
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate

@Composable
fun TaskPager(
    initialDate: LocalDate,
    tasks: List<Task>,
    onDateChanged: (LocalDate) -> Unit,
    onToggleTodo: (Long) -> Unit,
    onDeleteTask: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    // 현재 날짜의 해당 주 월요일 구하기
    var currentWeekMonday by remember {
        mutableStateOf(initialDate.with(DayOfWeek.MONDAY))
    }

    // 현재 주의 모든 날짜 (월~일)
    val weekDates = remember(currentWeekMonday) {
        (0..6).map { currentWeekMonday.plusDays(it.toLong()) }
    }

    // 표시할 날짜와 가장 가까운 인덱스 찾기
    val initialPageIndex = remember(weekDates, initialDate) {
        weekDates.indexOfFirst { it.isEqual(initialDate) }.takeIf { it >= 0 } ?: 0
    }

    val pagerState = rememberPagerState(
        initialPage = initialPageIndex,
        pageCount = { 7 }
    )

    Box(modifier = modifier) {
        // 페이저
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
                onToggleTodo = onToggleTodo,
                onDeleteTask = onDeleteTask
            )
        }

        TaskPageIndicator(
            pagerState = pagerState,
            currentWeekMonday = currentWeekMonday,
            weekDates = weekDates,
            onDateChanged = onDateChanged,
            onUpdateCurrentWeekMonday = { previousWeekMonday ->
                currentWeekMonday = previousWeekMonday
            }

        )

    }

    // 페이지 변경 시 호출
    LaunchedEffect(pagerState.currentPage) {
        val newDate = weekDates[pagerState.currentPage]
        onDateChanged(newDate)
    }
}