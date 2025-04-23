package co.kr.checkmate.presentation.home.components.fab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ExpandableFab(
    isExpanded: Boolean,
    onExpandChange: (Boolean) -> Unit,
    onAddTodo: () -> Unit,
    onAddMemo: () -> Unit
) {
    Box(
        contentAlignment = Alignment.BottomEnd
    ) {
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 72.dp, end = 4.dp)
            ) {
                FabMenuItem(
                    icon = Icons.Default.Check,
                    label = "투두",
                    onClick = {
                        onAddTodo()
                        onExpandChange(false)
                    }
                )

                FabMenuItem(
                    icon = Icons.Default.Edit,
                    label = "메모",
                    onClick = {
                        onAddMemo()
                        onExpandChange(false)
                    }
                )
            }
        }

        // 메인 FAB
        FloatingActionButton(
            onClick = { onExpandChange(!isExpanded) },
            containerColor = Color(0xFF4285F4),
            modifier = Modifier.shadow(8.dp)
        ) {
            val rotation by animateFloatAsState(
                targetValue = if (isExpanded) 45f else 0f,
                label = "fabRotation"
            )
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = if (isExpanded) "접기" else "펼치기",
                modifier = Modifier.rotate(rotation),
                tint = Color.White
            )
        }
    }
}