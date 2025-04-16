package co.kr.checkmate.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import co.kr.checkmate.presentation.home.HomeScreen
import co.kr.checkmate.presentation.home.HomeViewModel
import co.kr.checkmate.presentation.theme.CheckMateTheme
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val homeViewModel: HomeViewModel by viewModel()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CheckMateTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val homeState by homeViewModel.state.collectAsState()

                    HomeScreen(
                        state = homeState,
                        sideEffect = homeViewModel.sideEffect,
                        onEvent = homeViewModel::onEvent,
                    )
                }
            }
        }
    }
}
