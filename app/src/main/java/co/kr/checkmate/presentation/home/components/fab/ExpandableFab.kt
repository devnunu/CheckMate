package co.kr.checkmate.presentation.home.components.fab

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.kr.checkmate.presentation.home.HomeViewEvent

@Composable
fun ExpandableFab(
    isExpanded: Boolean,
    onEvent: (HomeViewEvent) -> Unit
) {
    Box(
        contentAlignment = Alignment.BottomEnd
    ) {
        // 메뉴 아이템들
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(animationSpec = tween(200)),
            exit = fadeOut(animationSpec = tween(200))
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
                        onEvent(HomeViewEvent.OnClickAddTodoBtn)
                        onEvent(HomeViewEvent.OnCollapseFab)
                    }
                )

                FabMenuItem(
                    icon = Icons.Default.Edit,
                    label = "메모",
                    onClick = {
                        onEvent(HomeViewEvent.OnClickAddMemoBtn)
                        onEvent(HomeViewEvent.OnCollapseFab)
                    }
                )
            }
        }

        // 메인 FAB
        FloatingActionButton(
            onClick = {
                val expanded = !isExpanded
                if (expanded) {
                    onEvent(HomeViewEvent.OnExpandFab)
                } else {
                    onEvent(HomeViewEvent.OnCollapseFab)
                }
            },
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = Color.White,
            modifier = Modifier.size(56.dp)
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
