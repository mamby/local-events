package net.mamby.events

import android.app.Application
import android.content.Context
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.request.crossfade
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import net.mamby.events.core.TelemetryRepository

@HiltAndroidApp
class LocalEventsApplication : Application(), SingletonImageLoader.Factory, DefaultLifecycleObserver {
    @Inject
    lateinit var telemetryRepository: TelemetryRepository

    override fun onCreate() {
        super<Application>.onCreate()
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    override fun onStart(owner: LifecycleOwner) {
        telemetryRepository.start()
    }

    override fun onStop(owner: LifecycleOwner) {
        telemetryRepository.stop()
    }

    override fun newImageLoader(context: Context): ImageLoader =
        ImageLoader.Builder(context)
            .crossfade(false)
            .build()
}
