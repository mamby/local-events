package net.mamby.events

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import net.mamby.events.core.AppThemePreference
import net.mamby.events.core.RootViewModel
import net.mamby.events.features.favorites.FavoritesScreen
import net.mamby.events.features.feed.FeedScreen
import net.mamby.events.features.feed.FeedViewModel
import net.mamby.events.features.settings.SettingsScreen
import net.mamby.events.features.settings.AppInfoScreen
import net.mamby.events.ui.LocalEventsTheme

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private val feedViewModel: FeedViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)
        val initialFeedViewModel = feedViewModel
        initialFeedViewModel.load()
        splashScreen.setKeepOnScreenCondition {
            !initialFeedViewModel.state.value.hasCompletedInitialCacheRead
        }
        enableEdgeToEdge()

        setContent {
            LocalEventsApp(feedViewModel = initialFeedViewModel)
        }
    }
}

private object AppRoute {
    const val Feed = "feed"
    const val Settings = "settings"
    const val AppInfo = "app-info"
    const val Favorites = "favorites"
}

@Composable
private fun LocalEventsApp(
    feedViewModel: FeedViewModel,
    rootViewModel: RootViewModel = hiltViewModel()
) {
    val settings by rootViewModel.settings.collectAsStateWithLifecycle()
    val configurationLocaleTags = LocalConfiguration.current.locales.toLanguageTags()
    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (settings.themePreference) {
        AppThemePreference.Light -> false
        AppThemePreference.Dark -> true
        AppThemePreference.System -> systemDark
    }

    val navController = rememberNavController()

    LaunchedEffect(configurationLocaleTags) {
        rootViewModel.refreshLocaleState()
    }

    LocalEventsTheme(darkTheme = darkTheme, floatingSurfaceOpacityLevel = settings.floatingSurfaceOpacityLevel) {
        NavHost(
            navController = navController,
            startDestination = AppRoute.Feed,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None },
            predictivePopEnterTransition = { EnterTransition.None },
            predictivePopExitTransition = { ExitTransition.KeepUntilTransitionsFinished },
        ) {
            composable(AppRoute.Feed) {
                FeedScreen(
                    language = settings.effectiveLanguage,
                    mediaAutoplayEnabled = settings.mediaAutoplayEnabled,
                    darkTheme = darkTheme,
                    onOpenSettings = { navController.navigate(AppRoute.Settings) },
                    onOpenFavorites = { navController.navigate(AppRoute.Favorites) },
                    viewModel = feedViewModel
                )
            }

            composable(AppRoute.Settings) {
                SettingsScreen(
                    onAppInfo = { navController.navigate(AppRoute.AppInfo) },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(AppRoute.AppInfo) {
                AppInfoScreen(onBack = { navController.popBackStack() })
            }

            composable(AppRoute.Favorites) {
                FavoritesScreen(
                    language = settings.effectiveLanguage,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
