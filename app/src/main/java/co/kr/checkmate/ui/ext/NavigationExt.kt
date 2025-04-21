package co.kr.checkmate.ui.ext

import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.reflect.KType

enum class ScreenAnim {
    HORIZONTAL_SLIDE,
    VERTICAL_SLIDE,
    FADE_IN_OUT,
}

inline fun <reified T : Any> NavGraphBuilder.animComposable(
    screenAnim: ScreenAnim = ScreenAnim.HORIZONTAL_SLIDE,
    typeMap: Map<KType, @JvmSuppressWildcards NavType<*>> = emptyMap(),
    noinline content: @Composable (AnimatedContentScope.(NavBackStackEntry) -> Unit)
) {
    composable<T>(
        enterTransition = {
            when (screenAnim) {
                ScreenAnim.HORIZONTAL_SLIDE -> {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Start,
                        animationSpec = tween(durationMillis = 400)
                    )
                }

                ScreenAnim.VERTICAL_SLIDE -> {
                    slideIntoContainer(
                        AnimatedContentTransitionScope.SlideDirection.Up,
                        animationSpec = tween(durationMillis = 400)
                    )
                }

                ScreenAnim.FADE_IN_OUT -> fadeIn()
            }
        },
        exitTransition = {
            when (screenAnim) {
                ScreenAnim.HORIZONTAL_SLIDE -> null
                ScreenAnim.VERTICAL_SLIDE -> null
                ScreenAnim.FADE_IN_OUT -> null
            }
        },
        popEnterTransition = {
            when (screenAnim) {
                ScreenAnim.HORIZONTAL_SLIDE -> null
                ScreenAnim.VERTICAL_SLIDE -> null
                ScreenAnim.FADE_IN_OUT -> null
            }
        },
        popExitTransition = {
            when (screenAnim) {
                ScreenAnim.HORIZONTAL_SLIDE -> {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.End,
                        animationSpec = tween(durationMillis = 400)
                    )
                }

                ScreenAnim.VERTICAL_SLIDE -> {
                    slideOutOfContainer(
                        AnimatedContentTransitionScope.SlideDirection.Down,
                        animationSpec = tween(durationMillis = 400)
                    )
                }

                ScreenAnim.FADE_IN_OUT -> {
                    fadeOut()
                }
            }
        },
        typeMap = typeMap,
        content = content
    )
}

inline fun <reified T : Parcelable> parcelableType(
    isNullableAllowed: Boolean = false,
    json: Json = Json,
) = object : NavType<T>(isNullableAllowed = isNullableAllowed) {
    override fun get(bundle: Bundle, key: String) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            bundle.getParcelable(key, T::class.java)
        } else {
            @Suppress("DEPRECATION")
            bundle.getParcelable(key)
        }

    override fun parseValue(value: String): T = json.decodeFromString(value)

    override fun serializeAsValue(value: T): String = Uri.encode(json.encodeToString(value))

    override fun put(bundle: Bundle, key: String, value: T) = bundle.putParcelable(key, value)
}