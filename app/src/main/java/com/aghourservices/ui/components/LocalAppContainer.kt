package com.aghourservices.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.aghourservices.di.AppContainer

/** Gives every screen access to the app's repositories (provided in MainActivity). */
val LocalAppContainer = staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}

@Composable
fun appContainer(): AppContainer = LocalAppContainer.current
