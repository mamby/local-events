package net.mamby.events.core.di

import dagger.Module
import dagger.Binds
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import io.ktor.client.HttpClient
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.defaultRequest
import io.ktor.http.URLProtocol
import io.ktor.http.takeFrom
import io.ktor.serialization.kotlinx.json.json
import javax.inject.Singleton
import javax.inject.Qualifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.serialization.json.Json
import net.mamby.events.BuildConfig
import net.mamby.events.core.AndroidAppLocaleController
import net.mamby.events.core.AppDateFormatter
import net.mamby.events.core.AppLocaleController
import net.mamby.events.core.AppLocalizer
import net.mamby.events.core.AppTextProvider
import net.mamby.events.core.DateTextFormatter
import net.mamby.events.core.FavoritesRepository
import net.mamby.events.core.FavoritesStore
import net.mamby.events.core.FeedApi
import net.mamby.events.core.FeedCacheRepository
import net.mamby.events.core.FeedCacheStore
import net.mamby.events.core.FeedRemoteDataSource
import net.mamby.events.core.SearchHistoryRepository
import net.mamby.events.core.SearchHistoryStore
import net.mamby.events.core.SearchDraftRepository
import net.mamby.events.core.SearchDraftStore
import net.mamby.events.core.SettingsRepository
import net.mamby.events.core.SettingsStore
import net.mamby.events.core.TelemetryRecorder
import net.mamby.events.core.TelemetryRepository
import net.mamby.events.core.localEventsDataStore
import dagger.hilt.android.qualifiers.ApplicationContext

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideJson(): Json =
        Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
        }

    @Provides
    @Singleton
    fun providePreferencesDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> =
        context.localEventsDataStore

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    @Provides
    @Singleton
    fun provideHttpClient(json: Json): HttpClient =
        HttpClient(Android) {
            install(ContentNegotiation) {
                json(json)
            }

            install(HttpTimeout) {
                requestTimeoutMillis = 10_000
                connectTimeoutMillis = 10_000
                socketTimeoutMillis = 10_000
            }

            defaultRequest {
                url.takeFrom(BuildConfig.LOCAL_EVENTS_API_BASE_URL)
                if (url.protocol.name.isBlank()) {
                    url.protocol = URLProtocol.HTTPS
                }
            }
        }
}

@Module
@InstallIn(SingletonComponent::class)
abstract class AppBindings {
    @Binds
    abstract fun bindAppLocaleController(implementation: AndroidAppLocaleController): AppLocaleController

    @Binds
    abstract fun bindAppTextProvider(implementation: AppLocalizer): AppTextProvider

    @Binds
    abstract fun bindDateTextFormatter(implementation: AppDateFormatter): DateTextFormatter

    @Binds
    abstract fun bindSettingsStore(implementation: SettingsRepository): SettingsStore

    @Binds
    abstract fun bindFeedRemoteDataSource(implementation: FeedApi): FeedRemoteDataSource

    @Binds
    abstract fun bindTelemetryRecorder(implementation: TelemetryRepository): TelemetryRecorder

    @Binds
    abstract fun bindFeedCacheStore(implementation: FeedCacheRepository): FeedCacheStore

    @Binds
    abstract fun bindFavoritesStore(implementation: FavoritesRepository): FavoritesStore

    @Binds
    abstract fun bindSearchHistoryStore(implementation: SearchHistoryRepository): SearchHistoryStore

    @Binds
    abstract fun bindSearchDraftStore(implementation: SearchDraftRepository): SearchDraftStore
}
