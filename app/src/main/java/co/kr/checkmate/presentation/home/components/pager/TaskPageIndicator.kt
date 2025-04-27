package co.kr.checkmate.presentation.home.components.pager

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate

@Composable
fun BoxScope.TaskPageIndicator(
    pagerState: PagerState,
    currentWeekMonday: LocalDate,
    weekDates: List<LocalDate>,
    onDateChanged: (LocalDate) -> Unit,
    onUpdateCurrentWeekMonday: (LocalDate) -> Unit
) {

    val coroutineScope = rememberCoroutineScope()

    // 오늘 날짜
    val today = LocalDate.now()

    // 오늘 날짜의 주 월요일
    val todayWeekMonday = today.with(DayOfWeek.MONDAY)

    // 현재 표시 중인 주가 오늘이 속한 주인지 확인
    val isCurrentWeekTodayWeek = currentWeekMonday.isEqual(todayWeekMonday)

    // 페이지 네비게이션 컨트롤 - 고정된 크기와 배치로 설정
    Box(
        modifier = Modifier
            .align(Alignment.BottomStart)
            .fillMaxWidth()
            .padding(bottom = 16.dp)
            .height(48.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 왼쪽 화살표 영역 (고정 크기)
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                // 월요일에만 왼쪽 화살표 표시
                if (pagerState.currentPage == 0) {
                    ElevatedButton(
                        onClick = {
                            // 저번주 일요일로 이동
                            val previousWeekSunday = currentWeekMonday.minusDays(1)
                            onDateChanged(previousWeekSunday)
                            // 현재 주 갱신
                            onUpdateCurrentWeekMonday(previousWeekSunday.with(DayOfWeek.MONDAY))
                            // 페이저 페이지를 6(일요일)로 설정
                            coroutineScope.launch {
                                pagerState.scrollToPage(6)
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "이전 주",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // 중앙 - 인디케이터 점
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                weekDates.forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = if (pagerState.currentPage == index)
                                    MaterialTheme.colorScheme.primary
                                else
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                shape = CircleShape
                            )
                    )
                }

                // "오늘" 버튼 - 인디케이터 바로 옆에 배치
                if (!isCurrentWeekTodayWeek) {
                    Text(
                        modifier = Modifier
                            .clickable {
                                // 오늘이 있는 주로 이동
                                onUpdateCurrentWeekMonday(todayWeekMonday)
                                onDateChanged(today)
                                coroutineScope.launch {
                                    // 오늘의 인덱스 계산 (월요일부터 0, 일요일은 6)
                                    val todayIndex = today.dayOfWeek.value - 1
                                    pagerState.scrollToPage(todayIndex)
                                }
                            },
                        text = "오늘",
                        style = MaterialTheme.typography.labelLarge.copy(
                            textDecoration = TextDecoration.Underline
                        ),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            // 오른쪽 화살표 영역 (고정 크기)
            Box(
                modifier = Modifier.size(48.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                // 일요일에만 오른쪽 화살표 표시
                if (pagerState.currentPage == 6) {
                    ElevatedButton(
                        onClick = {
                            // 다음주 월요일로 이동
                            val nextWeekMonday = currentWeekMonday.plusDays(7)
                            onDateChanged(nextWeekMonday)
                            // 현재 주 갱신
                            onUpdateCurrentWeekMonday(nextWeekMonday)
                            // 페이저 페이지를 0(월요일)로 설정
                            coroutineScope.launch {
                                pagerState.scrollToPage(0)
                            }
                        },
                        shape = CircleShape,
                        colors = ButtonDefaults.elevatedButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "다음 주",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
    }
}