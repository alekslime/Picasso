package app.picasso.tracer.ui.nav

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.picasso.tracer.ui.home.HomeScreen
import app.picasso.tracer.ui.trace.TraceScreen

object Routes {
    const val HOME = "home"
    const val TRACE = "trace"

    fun trace(projectId: Long) = "$TRACE?projectId=$projectId"
}

@Composable
fun PicassoNav() {
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(onOpenProject = { id -> nav.navigate(Routes.trace(id)) })
        }
        composable(
            route = "${Routes.TRACE}?projectId={projectId}",
            arguments = listOf(navArgument("projectId") { type = NavType.LongType }),
        ) {
            TraceScreen(onBack = { nav.popBackStack() })
        }
    }
}
