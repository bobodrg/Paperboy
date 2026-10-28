package ro.bogdandragomir.paperboy.navigation

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import ro.bogdandragomir.paperboy.brief.BriefRoute
import ro.bogdandragomir.paperboy.designsystem.LocalNavAnimatedVisibilityScope
import ro.bogdandragomir.paperboy.designsystem.LocalSharedTransitionScope
import ro.bogdandragomir.paperboy.designsystem.PaperboyIcons
import ro.bogdandragomir.paperboy.onboarding.OnboardingRoute
import ro.bogdandragomir.paperboy.profile.ProfileRoute
import ro.bogdandragomir.paperboy.saved.SavedRoute
import ro.bogdandragomir.paperboy.search.SearchRoute
import ro.bogdandragomir.paperboy.story.StoryRoute
import kotlin.reflect.KClass

@Serializable
object TodayDestination

@Serializable
object SearchDestination

@Serializable
object SavedDestination

@Serializable
object ProfileDestination

@Serializable
data class StoryDestination(
    val storyId: String,
)

@Serializable
object TopicsDestination

/** The four sections in the bottom bar. */
private enum class TopLevel(
    val route: Any,
    val routeClass: KClass<*>,
    val label: String,
    val icon: ImageVector,
) {
    Today(TodayDestination, TodayDestination::class, "Today", PaperboyIcons.Today),
    Search(SearchDestination, SearchDestination::class, "Search", PaperboyIcons.Search),
    Saved(SavedDestination, SavedDestination::class, "Saved", PaperboyIcons.Bookmark),
    Profile(ProfileDestination, ProfileDestination::class, "Profile", PaperboyIcons.Person),
}

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun PaperboyNavHost() {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    // The bar belongs to the four sections; full-screen pages (an article, the topic editor) hide it.
    val showBottomBar = TopLevel.entries.any { tab -> currentDestination?.hasRoute(tab.routeClass) == true }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                    TopLevel.entries.forEach { tab ->
                        val selected = currentDestination?.hierarchy?.any { it.hasRoute(tab.routeClass) } == true
                        NavigationBarItem(
                            selected = selected,
                            onClick = { navController.navigateToTab(tab.route) },
                            icon = {
                                val scale by animateFloatAsState(
                                    targetValue = if (selected) 1.15f else 1f,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                )
                                Icon(tab.icon, contentDescription = null, modifier = Modifier.scale(scale))
                            },
                            label = { Text(tab.label) },
                            colors =
                                NavigationBarItemDefaults.colors(
                                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                                    indicatorColor = MaterialTheme.colorScheme.primary,
                                ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                PaperboyDestinations(navController, Modifier.padding(bottom = padding.calculateBottomPadding()))
            }
        }
    }
}

@Composable
private fun PaperboyDestinations(
    navController: NavHostController,
    modifier: Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = TodayDestination,
        modifier = modifier,
        enterTransition = { fadeIn(tween(220)) },
        exitTransition = { fadeOut(tween(180)) },
    ) {
        destination<TodayDestination> {
            BriefRoute(onOpenStory = { navController.navigate(StoryDestination(it)) })
        }
        destination<SearchDestination> {
            SearchRoute(
                onOpenStory = { navController.navigate(StoryDestination(it)) },
            )
        }
        destination<SavedDestination> { SavedRoute(onOpenStory = { navController.navigate(StoryDestination(it)) }) }
        destination<ProfileDestination> {
            ProfileRoute(
                onEditTopics = { navController.navigate(TopicsDestination) },
                onOpenSaved = { navController.navigateToTab(SavedDestination) },
            )
        }
        destination<StoryDestination> { entry ->
            StoryRoute(
                storyId = entry.toRoute<StoryDestination>().storyId,
                onBack = { navController.popBackStack() },
            )
        }
        destination<TopicsDestination> {
            OnboardingRoute(isEditing = true, onDone = { navController.popBackStack() })
        }
    }
}

/** Switching tabs keeps one copy of each section and remembers where you were in it. */
private fun NavHostController.navigateToTab(route: Any) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** A destination whose content can take part in shared-element transitions (card → article). */
private inline fun <reified T : Any> NavGraphBuilder.destination(
    noinline content: @Composable (NavBackStackEntry) -> Unit,
) {
    composable<T> { entry ->
        CompositionLocalProvider(LocalNavAnimatedVisibilityScope provides this) { content(entry) }
    }
}
