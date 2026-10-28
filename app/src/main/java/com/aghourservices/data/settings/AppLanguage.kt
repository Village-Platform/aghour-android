package com.aghourservices.data.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat

enum class Language(val tag: String) { Arabic("ar"), English("en") }

/**
 * The app's language. Android stores the choice itself (per-app language, Android 13+; AppCompat's
 * autoStoreLocales on older versions), so it is not kept in SettingsStore: one place, no copy to drift.
 * With no choice made the app follows the phone: English phones get English, every other language
 * gets the Arabic default resources.
 */
object AppLanguage {

    fun current(): Language {
        val locales = AppCompatDelegate.getApplicationLocales().takeUnless { it.isEmpty }
            ?: LocaleListCompat.getAdjustedDefault()
        return if (locales[0]?.language == Language.English.tag) Language.English else Language.Arabic
    }

    /** Applies at once: Android recreates the activity in the new language and direction. */
    fun set(language: Language) {
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language.tag))
    }
}
