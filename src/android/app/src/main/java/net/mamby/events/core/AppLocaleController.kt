package net.mamby.events.core

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.LocaleManagerCompat
import androidx.core.os.LocaleListCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

interface AppLocaleController {
    fun selectedLanguageTag(): String?

    fun systemLanguageTag(): String

    fun effectiveLanguageTag(): String

    fun setSelectedLanguageTag(languageTag: String?)
}

@Singleton
class AndroidAppLocaleController @Inject constructor(
    @param:ApplicationContext private val context: Context
) : AppLocaleController {
    override fun selectedLanguageTag(): String? =
        AppCompatDelegate.getApplicationLocales()
            .toLanguageTags()
            .takeIf(String::isNotBlank)
            ?.let(::normalizeAppLanguageTag)

    override fun systemLanguageTag(): String =
        normalizeAppLanguageTag(
            LocaleManagerCompat.getSystemLocales(context).get(0)?.toLanguageTag()
                ?: Locale.getDefault().toLanguageTag()
        )

    override fun effectiveLanguageTag(): String =
        normalizeAppLanguageTag(
            selectedLanguageTag() ?: systemLanguageTag()
        )

    override fun setSelectedLanguageTag(languageTag: String?) {
        val localeList = languageTag
            ?.takeIf { isSupportedAppLanguageTag(it) }
            ?.let { LocaleListCompat.forLanguageTags(normalizeAppLanguageTag(it)) }
            ?: LocaleListCompat.getEmptyLocaleList()

        AppCompatDelegate.setApplicationLocales(localeList)
    }
}
