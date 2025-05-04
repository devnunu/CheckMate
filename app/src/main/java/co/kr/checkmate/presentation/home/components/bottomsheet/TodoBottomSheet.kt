package co.kr.checkmate.presentation.home.components.bottomsheet

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.kr.checkmate.presentation.home.HomeBottomSheetTag
import co.kr.checkmate.presentation.home.HomeState
import co.kr.checkmate.presentation.home.HomeViewEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoBottomSheet(
    state: HomeState,
    onEvent: (HomeViewEvent) -> Unit,
) {
    val todoTag = state.bottomSheetState.tag as? HomeBottomSheetTag.Todo
    val isEditMode = todoTag?.isEditMode == true
    val selectedTodo = todoTag?.selectedTodo

    var title by remember {
        mutableStateOf(selectedTodo?.title ?: "")
    }

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
            IconButton(onClick = { onEvent(HomeViewEvent.OnClickCloseBottomSheet) }) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "닫기"
                )
            }

            Text(
                text = if (isEditMode) "투두 수정" else "투두 추가",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )

            if (isEditMode) {
                // 수정 모드일 때 삭제 버튼 추가
                Button(
                    onClick = {
                        selectedTodo?.let { todo ->
                            onEvent(HomeViewEvent.OnClickDeleteTodo(todo.id))
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "삭제"
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("삭제")
                }

                Spacer(modifier = Modifier.width(8.dp))
            }

            Button(
                onClick = {
                    if (isEditMode) {
                        selectedTodo?.let { todo ->
                            onEvent(HomeViewEvent.OnUpdateTodo(todo.id, title))
                        }
                    } else {
                        onEvent(HomeViewEvent.OnCreateTodo(title))
                    }
                },
                enabled = title.isNotBlank()
            ) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "저장"
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("저장")
            }
        }

        // 나머지 UI 부분 (기존 코드 유지)
        Spacer(modifier = Modifier.height(16.dp))

        // 입력 필드
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("할 일") },
            placeholder = { Text("할 일을 입력하세요") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        // 하단 여백
        Spacer(modifier = Modifier.height(32.dp))
    }
}