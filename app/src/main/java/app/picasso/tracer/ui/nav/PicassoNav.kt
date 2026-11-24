package app.picasso.tracer.ui.nav

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.picasso.tracer.ui.home.HomeScreen
import app.picasso.tracer.ui.settings.SettingsScreen
import app.picasso.tracer.ui.trace.TraceScreen

// Material 3 "emphasized" curves: fast start with a soft landing in, quick get-out-of-the-way going out.
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

object Routes {
    const val HOME = "home"
    const val TRACE = "trace"
    const val SETTINGS = "settings"

    fun trace(projectId: Long) = "$TRACE?projectId=$projectId"
}

@Composable
fun PicassoNav(
    openProjectId: Long? = null,
    onProjectConsumed: () -> Unit = {},
) {
    val nav = rememberNavController()
    LaunchedEffect(openProjectId) {
        val id = openProjectId ?: return@LaunchedEffect
        nav.navigate(Routes.trace(id))
        onProjectConsumed()
    }
    NavHost(
        navController = nav,
        startDestination = Routes.HOME,
        // Shared-axis feel: the new screen drifts in a short way while fading up, the old one drifts
        // out the other side. Short offsets keep it calm, and the delayed fade-in avoids a see-through gap.
        enterTransition = {
            slideInHorizontally(tween(360, easing = EmphasizedDecelerate)) { it / 5 } +
                fadeIn(tween(240, delayMillis = 60))
        },
        exitTransition = {
            slideOutHorizontally(tween(280, easing = EmphasizedAccelerate)) { -it / 8 } +
                fadeOut(tween(160))
        },
        popEnterTransition = {
            slideInHorizontally(tween(360, easing = EmphasizedDecelerate)) { -it / 5 } +
                fadeIn(tween(240, delayMillis = 60))
        },
        popExitTransition = {
            slideOutHorizontally(tween(280, easing = EmphasizedAccelerate)) { it / 8 } +
                fadeOut(tween(160))
        },
    ) {
        composable(Routes.HOME) {
            HomeScreen(
                onOpenProject = { id -> nav.navigate(Routes.trace(id)) },
                onOpenSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable(
            route = "${Routes.TRACE}?projectId={projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
        ) {
            TraceScreen(onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { nav.popBackStack() })
        }
    }
}
