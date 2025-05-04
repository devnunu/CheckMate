package co.kr.checkmate.presentation.home.components.date

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Redo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.kr.checkmate.presentation.home.HomeState
import co.kr.checkmate.presentation.home.HomeViewEvent
import co.kr.checkmate.ui.theme.blue10
import org.threeten.bp.format.DateTimeFormatter

@Composable
fun TopDateSection(
    state: HomeState,
    onEvent: (HomeViewEvent) -> Unit
) {
// 날짜 표시 부분 수정
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
            val date = state.selectedDate.format(DateTimeFormatter.ofPattern("dd일.E"))
            Text(
                text = date,
                style = MaterialTheme.typography.headlineLarge,
                textAlign = TextAlign.Start,
                color = blue10,
            )

            // 오늘 날짜인 경우 "Today" 표시
            if (state.isSelectedDateToday) {
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
        // 오늘이 아닌 날짜에서만 '오늘로 이동' 버튼 노출
        if (!state.isSelectedDateToday) {
            IconButton(
                onClick = { onEvent(HomeViewEvent.OnClickMoveTodosToToday) }
            ) {
                Icon(
                    imageVector = Icons.Default.Redo, // 적절한 아이콘으로 변경 필요
                    contentDescription = "할 일을 오늘로 이동",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}