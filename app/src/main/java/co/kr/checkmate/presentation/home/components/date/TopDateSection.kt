package co.kr.checkmate.presentation.home.components.date

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import co.kr.checkmate.ui.theme.blue10
import kotlinx.coroutines.launch
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter

@Composable
fun TopDateSection(
    pagerState: PagerState,
    currentWeekMonday: LocalDate,
    todayDate: LocalDate,
    selectedDate: LocalDate,
    onClickMoveTodosToToday: () -> Unit,
    onDateChanged: (LocalDate) -> Unit,
    onUpdateCurrentWeekMonday: (LocalDate) -> Unit
) {
    val coroutineScope = rememberCoroutineScope()

    val isSelectedDateToday = selectedDate.isEqual(LocalDate.now())

    // 오늘 날짜의 주 월요일
    val todayWeekMonday = todayDate.with(DayOfWeek.MONDAY)

    // 현재 표시 중인 주가 오늘이 속한 주인지 확인
    val isCurrentWeekTodayWeek = currentWeekMonday.isEqual(todayWeekMonday)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            val date = selectedDate.format(DateTimeFormatter.ofPattern("dd일.E"))
            Text(
                text = date,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Start,
                color = blue10,
            )

            // 오늘 날짜인 경우 "Today" 표시
            if (isSelectedDateToday) {
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

        Column(
            horizontalAlignment = Alignment.End
        ) {
            // 오늘이 아닌 날짜에서만 '오늘로 이동' 버튼 노출
            if (!isSelectedDateToday) {
                IconButton(
                    onClick = { onClickMoveTodosToToday() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Redo,
                        contentDescription = "할 일을 오늘로 이동",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 왼쪽 화살표 - 월요일에만 표시
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
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowLeft,
                            contentDescription = "이전 주",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // "오늘" 버튼
                if (!isCurrentWeekTodayWeek) {
                    Text(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .clickable {
                                // 오늘이 있는 주로 이동
                                onUpdateCurrentWeekMonday(todayWeekMonday)
                                onDateChanged(todayDate)
                                coroutineScope.launch {
                                    // 오늘의 인덱스 계산 (월요일부터 0, 일요일은 6)
                                    val todayIndex = todayDate.dayOfWeek.value - 1
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

                // 오른쪽 화살표 - 일요일에만 표시
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
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowRight,
                            contentDescription = "다음 주",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}