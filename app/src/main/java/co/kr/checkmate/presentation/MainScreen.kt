package co.kr.checkmate.presentation

import androidx.compose.runtime.Composable
import co.kr.checkmate.presentation.home.HomeScreen
import co.kr.checkmate.presentation.home.HomeViewModel
import org.koin.androidx.compose.koinViewModel

@Composable
fun MainScreen() {
    val homeViewModel: HomeViewModel = koinViewModel()

    // 홈 화면
    HomeScreen(
        viewModel = homeViewModel,
    )


}