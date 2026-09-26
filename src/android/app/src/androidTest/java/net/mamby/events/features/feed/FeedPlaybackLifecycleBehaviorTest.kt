package net.mamby.events.features.feed

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.exoplayer.ExoPlayer
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class FeedPlaybackLifecycleBehaviorTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun playback_pauses_in_background_and_resumes_in_foreground() {
        val lifecycleOwner = TestLifecycleOwner()
        lateinit var player: ExoPlayer

        composeRule.setContent {
            val context = LocalContext.current
            player = remember { ExoPlayer.Builder(context).build() }

            DisposableEffect(player) {
                onDispose {
                    player.release()
                }
            }

            CompositionLocalProvider(LocalLifecycleOwner provides lifecycleOwner) {
                FeedPlaybackLifecycleEffect(
                    player = player,
                    shouldPlay = true
                )
            }
        }

        composeRule.runOnIdle {
            lifecycleOwner.handle(Lifecycle.Event.ON_CREATE)
            lifecycleOwner.handle(Lifecycle.Event.ON_START)
            lifecycleOwner.handle(Lifecycle.Event.ON_RESUME)
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertTrue(player.playWhenReady)
            lifecycleOwner.handle(Lifecycle.Event.ON_PAUSE)
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertFalse(player.playWhenReady)
            lifecycleOwner.handle(Lifecycle.Event.ON_RESUME)
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            assertTrue(player.playWhenReady)
        }
    }
}

private class TestLifecycleOwner : LifecycleOwner {
    private val registry = LifecycleRegistry.createUnsafe(this)

    override val lifecycle: Lifecycle = registry

    fun handle(event: Lifecycle.Event) {
        registry.handleLifecycleEvent(event)
    }
}
