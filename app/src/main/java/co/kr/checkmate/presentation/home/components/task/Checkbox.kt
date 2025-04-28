package co.kr.checkmate.presentation.home.components.task

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
fun Checkbox(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: () -> Unit,
) {
    Box(
        modifier = modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(
                color = if (checked) Color(0xFF4CAF50) else MaterialTheme.colorScheme.surface,
                shape = CircleShape
            )
            .border(
                width = 2.dp,
                color = if (checked) Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline.copy(
                    alpha = 0.5f
                ),
                shape = CircleShape
            )
            .clickable { onCheckedChange() },
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "완료됨",
                tint = Color.White,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CheckboxPreview1() {
    Checkbox(
        checked = false,
        onCheckedChange = {}
    )
}

@Preview(showBackground = true)
@Composable
private fun CheckboxPreview2() {
    Checkbox(
        checked = true,
        onCheckedChange = {}
    )
}