package ro.bogdandragomir.paperboy.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable
import ro.bogdandragomir.paperboy.brief.BriefRoute
import ro.bogdandragomir.paperboy.designsystem.PaperboyIcons
import ro.bogdandragomir.paperboy.onboarding.OnboardingRoute
import ro.bogdandragomir.paperboy.profile.ProfileRoute
import ro.bogdandragomir.paperboy.saved.SavedRoute
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
                            icon = { Icon(tab.icon, contentDescription = null) },
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
        NavHost(
            navController = navController,
            startDestination = TodayDestination,
            modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
        ) {
            composable<TodayDestination> {
                BriefRoute(onOpenStory = { navController.navigate(StoryDestination(it)) })
            }
            composable<SearchDestination> { ComingSoon("Search") }
            composable<SavedDestination> { SavedRoute(onOpenStory = { navController.navigate(StoryDestination(it)) }) }
            composable<ProfileDestination> {
                ProfileRoute(
                    onEditTopics = { navController.navigate(TopicsDestination) },
                    onOpenSaved = { navController.navigateToTab(SavedDestination) },
                )
            }
            composable<StoryDestination> { entry ->
                StoryRoute(
                    storyId = entry.toRoute<StoryDestination>().storyId,
                    onBack = { navController.popBackStack() },
                )
            }
            composable<TopicsDestination> {
                OnboardingRoute(isEditing = true, onDone = { navController.popBackStack() })
            }
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

@Composable
private fun ComingSoon(title: String) {
    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
        Text("$title is on its way.", style = MaterialTheme.typography.titleLarge)
    }
}
