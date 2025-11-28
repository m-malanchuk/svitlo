package com.malanchuk.svitlo.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

fun Context.withLanguage(languageTag: String): Context {
    val locale = Locale.forLanguageTag(languageTag)
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(locale)
    return createConfigurationContext(configuration)
}
