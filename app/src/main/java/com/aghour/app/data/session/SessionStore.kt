package com.aghour.app.data.session

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aghour.app.data.remote.UserDto
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

private val Context.sessionDataStore by preferencesDataStore(name = "session")

/** Keeps the JWT and the signed-in user on disk. Later: move the token to encrypted storage. */
class SessionStore(context: Context, private val json: Json, scope: CoroutineScope) {

    private val store = context.applicationContext.sessionDataStore

    val token: StateFlow<String?> = store.data
        .map { it[TOKEN] }
        .stateIn(scope, SharingStarted.Eagerly, null)

    val user: StateFlow<UserDto?> = store.data
        .map { prefs -> prefs[USER]?.let { runCatching { json.decodeFromString<UserDto>(it) }.getOrNull() } }
        .stateIn(scope, SharingStarted.Eagerly, null)

    /** null while loading from disk, then true once the welcome screen was completed. */
    val welcomeDone: StateFlow<Boolean?> = store.data
        .map { it[WELCOME_DONE] ?: false }
        .stateIn(scope, SharingStarted.Eagerly, null)

    suspend fun setWelcomeDone() {
        store.edit { it[WELCOME_DONE] = true }
    }

    suspend fun save(token: String, user: UserDto) {
        store.edit {
            it[TOKEN] = token
            it[USER] = json.encodeToString(UserDto.serializer(), user)
        }
    }

    suspend fun updateUser(user: UserDto) {
        store.edit { it[USER] = json.encodeToString(UserDto.serializer(), user) }
    }

    /** Signs out but keeps app preferences such as the welcome flag. */
    suspend fun clear() {
        store.edit {
            it.remove(TOKEN)
            it.remove(USER)
        }
    }

    private companion object {
        val TOKEN = stringPreferencesKey("token")
        val USER = stringPreferencesKey("user")
        val WELCOME_DONE = booleanPreferencesKey("welcome_done")
    }
}
