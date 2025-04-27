package co.kr.checkmate.presentation.home.components.task

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.kr.checkmate.domain.model.Task
import kotlinx.coroutines.launch
import org.threeten.bp.DayOfWeek
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

    val coroutineScope = rememberCoroutineScope()

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

        // 페이지 인디케이터와 화살표 버튼을 Box로 감싸서 레이아웃 안정화
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, bottom = 16.dp)
                .height(48.dp)  // 고정 높이를 지정하여 안정화
        ) {
            Row(
                modifier = Modifier.align(Alignment.CenterStart),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 왼쪽 화살표 영역 (고정 크기)
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 월요일에만 왼쪽 화살표 표시
                    if (pagerState.currentPage == 0) {
                        IconButton(
                            onClick = {
                                // 저번주 일요일로 이동
                                val previousWeekSunday = currentWeekMonday.minusDays(1)
                                onDateChanged(previousWeekSunday)
                                // 현재 주 갱신
                                currentWeekMonday = previousWeekSunday.with(DayOfWeek.MONDAY)
                                // 페이저 페이지를 6(일요일)로 설정
                                coroutineScope.launch {
                                    pagerState.scrollToPage(6)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowLeft,
                                contentDescription = "이전 주",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // 페이지 인디케이터
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(horizontal = 8.dp)
                ) {
                    weekDates.forEachIndexed { index, _ ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(
                                    color = if (pagerState.currentPage == index)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    shape = CircleShape
                                )
                        )
                    }
                }

                // 오른쪽 화살표 영역 (고정 크기)
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // 일요일에만 오른쪽 화살표 표시
                    if (pagerState.currentPage == 6) {
                        IconButton(
                            onClick = {
                                // 다음주 월요일로 이동
                                val nextWeekMonday = currentWeekMonday.plusDays(7)
                                onDateChanged(nextWeekMonday)
                                // 현재 주 갱신
                                currentWeekMonday = nextWeekMonday
                                // 페이저 페이지를 0(월요일)로 설정
                                coroutineScope.launch {
                                    pagerState.scrollToPage(0)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowRight,
                                contentDescription = "다음 주",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }

    // 페이지 변경 시 호출
    LaunchedEffect(pagerState.currentPage) {
        val newDate = weekDates[pagerState.currentPage]
        onDateChanged(newDate)
    }
}