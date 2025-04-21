package co.kr.checkmate.presentation.todo

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.Flow
import org.threeten.bp.Instant
import org.threeten.bp.LocalDate
import org.threeten.bp.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoBottomSheet(
    onDismiss: () -> Unit,
    initialDate: LocalDate,
    viewModel: TodoViewModel,
    sideEffect: Flow<TodoSideEffect>
) {
    val state by viewModel.state.collectAsState()
    val sheetState = rememberModalBottomSheetState()
    var showDatePicker by remember { mutableStateOf(false) }

    // 초기 날짜 설정
    LaunchedEffect(key1 = initialDate) {
        viewModel.processEvent(TodoViewEvent.SetDate(initialDate))
    }

    // 사이드 이펙트 처리
    LaunchedEffect(key1 = true) {
        sideEffect.collect { effect ->
            when (effect) {
                is TodoSideEffect.TodoSaved -> onDismiss()
                is TodoSideEffect.Dismissed -> onDismiss()
                is TodoSideEffect.ShowError -> {
                    // 에러 처리 (스낵바 등으로 표시 가능)
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = { viewModel.processEvent(TodoViewEvent.Dismiss) },
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.processEvent(TodoViewEvent.Dismiss) }) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "닫기"
                    )
                }

                Text(
                    text = "투두 추가",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = { viewModel.processEvent(TodoViewEvent.SaveTodo) },
                    enabled = state.title.isNotBlank()
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "저장"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("저장")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 입력 필드
            OutlinedTextField(
                value = state.title,
                onValueChange = { viewModel.processEvent(TodoViewEvent.UpdateTitle(it)) },
                label = { Text("할 일") },
                placeholder = { Text("할 일을 입력하세요") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // 날짜 선택
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "날짜: ${state.date.toString()}",
                    style = MaterialTheme.typography.bodyLarge
                )

                Spacer(modifier = Modifier.weight(1f))

                IconButton(onClick = { showDatePicker = true }) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "날짜 선택"
                    )
                }
            }

            // 하단 여백
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // 날짜 선택 다이얼로그
    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.date.atStartOfDay(ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        )

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val localDate = Instant.ofEpochMilli(millis)
                            .atZone(ZoneId.systemDefault())
                            .toLocalDate()
                        viewModel.processEvent(
                            TodoViewEvent.SetDate(
                                LocalDate.of(
                                    localDate.year, localDate.month, localDate.dayOfMonth
                                )
                            )
                        )
                    }
                    showDatePicker = false
                }) {
                    Text("확인")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("취소")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}