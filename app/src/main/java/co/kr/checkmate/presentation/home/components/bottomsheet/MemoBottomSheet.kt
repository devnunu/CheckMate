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
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
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
import co.kr.checkmate.presentation.home.HomeState
import co.kr.checkmate.presentation.home.HomeViewEvent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoBottomSheet(
    state: HomeState,
    onEvent: (HomeViewEvent) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }

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
                text = "메모 추가",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f)
            )

            Button(
                onClick = { onEvent(HomeViewEvent.OnCreateMemo(title, content)) },
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

        Spacer(modifier = Modifier.height(16.dp))

        // 제목 입력 필드
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("제목") },
            placeholder = { Text("메모 제목을 입력하세요") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 내용 입력 필드
        OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("내용") },
            placeholder = { Text("메모 내용을 입력하세요") },
            minLines = 4,
            maxLines = 8,
            modifier = Modifier.fillMaxWidth()
        )

        // 하단 여백
        Spacer(modifier = Modifier.height(32.dp))
    }
}