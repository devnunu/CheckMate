package co.kr.checkmate.presentation.home.components.date

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import co.kr.checkmate.ui.theme.gray80

@Composable
fun PreviousWeekButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val color = if (enabled) MaterialTheme.colorScheme.primary else gray80
    Row(
        modifier = Modifier
            .background(
                color = color,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clickable { if (enabled) onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowLeft,
            contentDescription = "저번주",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PreviousWeekButtonPreview() {
    PreviousWeekButton(
        enabled = true,
        onClick = {}
    )
}

@Composable
fun PostWeekButton(
    enabled: Boolean,
    onClick: () -> Unit
) {
    val color = if (enabled) MaterialTheme.colorScheme.primary else gray80
    Row(
        modifier = Modifier
            .background(
                color = color,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clickable { if (enabled) onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "다음주",
            tint = Color.White,
            modifier = Modifier.size(16.dp)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun PostWeekButtonPreview() {
    PostWeekButton(
        enabled = true,
        onClick = {}
    )
}