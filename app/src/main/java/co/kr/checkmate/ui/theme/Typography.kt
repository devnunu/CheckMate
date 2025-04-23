package co.kr.checkmate.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import co.kr.checkmate.R

val fonts = FontFamily(
    Font(R.font.pretendard_bold, weight = FontWeight.Bold),
    Font(R.font.pretendard_medium, weight = FontWeight.Medium),
    Font(R.font.pretendard_regular, weight = FontWeight.Normal),
    Font(R.font.pretendard_light, weight = FontWeight.Light),
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = fonts,
        fontWeight = FontWeight.Bold,   // 큰 디스플레이 텍스트는 Bold로 변경
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = fonts,
        fontWeight = FontWeight.Bold,   // 중간 디스플레이 텍스트도 Bold로 변경
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = fonts,
        fontWeight = FontWeight.Bold,   // 작은 디스플레이 텍스트도 Bold로 변경
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = fonts,
        fontWeight = FontWeight.Bold,   // 큰 헤드라인은 Bold로 변경
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = fonts,
        fontWeight = FontWeight.Bold,   // 중간 헤드라인도 Bold로 변경
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = fonts,
        fontWeight = FontWeight.Bold,   // 작은 헤드라인도 Bold로 변경
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = fonts,
        fontWeight = FontWeight.Bold,   // 큰 제목은 Bold로 변경
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = fonts,
        fontWeight = FontWeight.Bold,   // 중간 제목도 Bold로 변경
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = fonts,
        fontWeight = FontWeight.Medium, // 작은 제목은 Medium 유지
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = fonts,
        fontWeight = FontWeight.Normal, // 본문은 Normal 유지
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = fonts,
        fontWeight = FontWeight.Normal, // 본문은 Normal 유지
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = fonts,
        fontWeight = FontWeight.Normal, // 본문은 Normal 유지
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = fonts,
        fontWeight = FontWeight.Bold,   // 큰 라벨은 Bold로 변경
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = fonts,
        fontWeight = FontWeight.Medium, // 중간 라벨은 Medium 유지
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = fonts,
        fontWeight = FontWeight.Medium, // 작은 라벨은 Medium 유지
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)