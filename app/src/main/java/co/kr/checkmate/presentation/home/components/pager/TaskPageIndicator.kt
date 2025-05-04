package co.kr.checkmate.presentation.home.components.pager

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.threeten.bp.LocalDate

@Composable
fun PageIndicator(
    pagerState: PagerState,
    weekDates: List<LocalDate>
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 2dp 높이의 divider 스타일 7등분 indicator
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .height(2.dp)
        ) {
            // 7등분 (월~일)
            weekDates.forEachIndexed { index, _ ->
                // 각 요일 섹션
                Box(
                    modifier = Modifier
                        .weight(1f) // 동일한 비율로 7등분
                        .height(2.dp)
                        .background(
                            color = if (pagerState.currentPage == index)
                                MaterialTheme.colorScheme.primary // 선택된 페이지
                            else
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) // 선택되지 않은 페이지
                        )
                )
            }
        }
    }
}