// app/src/main/java/co/kr/checkmate/presentation/home/calendar/Calendar.kt

package co.kr.checkmate.presentation.home.calendar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.kr.checkmate.presentation.home.CalendarType
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.YearMonth
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.TextStyle
import java.util.Locale

@Composable
fun CalendarView(
    selectedDate: LocalDate,
    calendarType: CalendarType,
    onDateSelected: (LocalDate) -> Unit,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMonth = remember(selectedDate) { YearMonth.from(selectedDate) }

    Column(modifier = modifier) {
        // 현재 월 헤더와 좌우 버튼
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                val newDate = if (calendarType == CalendarType.MONTH) {
                    selectedDate.minusMonths(1).withDayOfMonth(1)
                } else {
                    selectedDate.minusWeeks(1)
                }
                onDateSelected(newDate)
            }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowLeft,
                    contentDescription = "이전"
                )
            }

            Text(
                text = if (calendarType == CalendarType.MONTH) {
                    currentMonth.format(DateTimeFormatter.ofPattern("yyyy년 MM월"))
                } else {
                    val weekStart = selectedDate.minusDays(selectedDate.dayOfWeek.value % 7L)
                    val weekEnd = weekStart.plusDays(6)
                    "${weekStart.format(DateTimeFormatter.ofPattern("MM월 dd일"))} - ${weekEnd.format(DateTimeFormatter.ofPattern("MM월 dd일"))}"
                },
                style = MaterialTheme.typography.titleMedium
            )

            IconButton(onClick = {
                val newDate = if (calendarType == CalendarType.MONTH) {
                    selectedDate.plusMonths(1).withDayOfMonth(1)
                } else {
                    selectedDate.plusWeeks(1)
                }
                onDateSelected(newDate)
            }) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "다음"
                )
            }
        }

        // 요일 헤더
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
        ) {
            for (dayOfWeek in DayOfWeek.values()) {
                Text(
                    text = dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN),
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 캘린더 내용
        CalendarContent(
            selectedDate = selectedDate,
            calendarType = calendarType,
            onDateSelected = onDateSelected
        )

        // 확장/축소 버튼
        IconButton(
            onClick = onExpandClick,
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 4.dp)
        ) {
            Icon(
                imageVector = if (calendarType == CalendarType.MONTH)
                    Icons.Default.KeyboardArrowUp
                else
                    Icons.Default.KeyboardArrowDown,
                contentDescription = if (calendarType == CalendarType.MONTH)
                    "캘린더 접기"
                else
                    "캘린더 펼치기"
            )
        }
    }
}

@Composable
fun CalendarContent(
    selectedDate: LocalDate,
    calendarType: CalendarType,
    onDateSelected: (LocalDate) -> Unit
) {
    val currentMonth = remember(selectedDate) { YearMonth.from(selectedDate) }
    val currentMonthDays = remember(currentMonth) { currentMonth.lengthOfMonth() }
    val firstDayOfMonth = remember(currentMonth) { currentMonth.atDay(1) }
    val firstDayOfWeek = remember(firstDayOfMonth) { firstDayOfMonth.dayOfWeek.value % 7 }

    // 선택된 날짜의 주 인덱스 계산
    val selectedWeekIndex = remember(selectedDate, firstDayOfMonth) {
        val dayOfMonth = selectedDate.dayOfMonth
        val dayPosition = dayOfMonth + firstDayOfWeek - 1
        dayPosition / 7
    }

    // 이번 달의 주 수 계산
    val weeksInMonth = remember(currentMonth) {
        val daysInMonth = currentMonth.lengthOfMonth()
        val firstDayOfWeekIndex = currentMonth.atDay(1).dayOfWeek.value % 7
        (daysInMonth + firstDayOfWeekIndex + 6) / 7
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // 모든 주를 표시하거나, 주간 모드일 때는 선택된 주만 표시
        for (week in 0 until weeksInMonth) {
            // 월간 보기일 때는 모든 주를 표시, 주간 보기일 때는 선택된 주만 표시
            val isVisible = calendarType == CalendarType.MONTH || week == selectedWeekIndex

            AnimatedVisibility(
                visible = isVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                WeekRow(
                    week = week,
                    firstDayOfMonth = firstDayOfMonth,
                    firstDayOfWeek = firstDayOfWeek,
                    daysInMonth = currentMonthDays,
                    today = LocalDate.now(),
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected
                )
            }
        }
    }
}

@Composable
fun WeekRow(
    week: Int,
    firstDayOfMonth: LocalDate,
    firstDayOfWeek: Int,
    daysInMonth: Int,
    today: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        for (dayOfWeek in 0..6) {
            val dayPosition = week * 7 + dayOfWeek
            val day = dayPosition - firstDayOfWeek + 1

            if (day in 1..daysInMonth) {
                val date = firstDayOfMonth.withDayOfMonth(day)
                val isSelected = date == selectedDate
                val isToday = date == today

                CalendarDay(
                    date = date,
                    isSelected = isSelected,
                    isToday = isToday,
                    onDateSelected = onDateSelected
                )
            } else {
                // 이번 달에 속하지 않는 날짜 (빈 공간)
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun RowScope.CalendarDay(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    onDateSelected: (LocalDate) -> Unit
) {
    Box(
        modifier = Modifier
            .weight(1f)
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(CircleShape)
            .background(
                when {
                    isSelected -> MaterialTheme.colorScheme.primary
                    isToday -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    else -> Color.Transparent
                }
            )
            .clickable { onDateSelected(date) },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = date.dayOfMonth.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = when {
                isSelected -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
}