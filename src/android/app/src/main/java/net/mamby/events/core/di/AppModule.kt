package net.mamby.events.core.di

import dagger.Module
import dagger.Binds
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.core.app.LocaleManagerCompat
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
import net.mamby.androidkit.compose.form.AndroidKitSettingsStore
import net.mamby.androidkit.compose.form.AndroidKitSettingsStoreMigration
import net.mamby.androidkit.compose.form.AndroidKitSettingsStorageProtection
import net.mamby.androidkit.compose.form.AndroidKitSearchHistorySnapshot
import kotlinx.coroutines.flow.first
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
        store: AndroidKitSettingsStore
    ): DataStore<Preferences> =
        store.preferences

    @Provides
    @Singleton
    fun provideKitSettingsStore(@ApplicationContext context: Context): AndroidKitSettingsStore =
        AndroidKitSettingsStore.open(context, "settings", AndroidKitSettingsStorageProtection.Plaintext,
            listOf(object : AndroidKitSettingsStoreMigration {
                override val id = "local-events-preferences-v1"
                override suspend fun readPreferences(): Preferences {
                    val preferences = context.localEventsDataStore.data.first().toMutablePreferences()
                    val legacyLanguage = when (preferences[stringPreferencesKey("settings.language")]) {
                        "English" -> "en"
                        "French" -> "fr"
                        "Arabic" -> "ar"
                        else -> "system"
                    }
                    preferences[stringPreferencesKey("selected_language_tag")] =
                        LocaleManagerCompat.getApplicationLocales(context).get(0)?.language ?: legacyLanguage
                    val autoplay = booleanPreferencesKey("settings.mediaAutoplayEnabled")
                    if (preferences[autoplay] == null) {
                        preferences[autoplay] = preferences[booleanPreferencesKey("settings.audioAutoplayEnabled")] ?: false
                    }
                    return preferences.toPreferences()
                }
                override suspend fun readHistories(): Map<String, AndroidKitSearchHistorySnapshot> = emptyMap()
                override suspend fun cleanUp() = Unit
            }),
        )

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
