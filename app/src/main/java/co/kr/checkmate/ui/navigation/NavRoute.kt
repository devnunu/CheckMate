package co.kr.checkmate.ui.navigation

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed class NavRoute {
    @Serializable
    @SerialName("Home")
    data object Home : NavRoute()


}