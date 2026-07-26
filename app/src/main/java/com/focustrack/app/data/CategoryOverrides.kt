package com.focustrack.app.data

import android.content.Context

/**
 * User-defined per-app category overrides, persisted in SharedPreferences.
 * An override takes precedence over [AppCategories] defaults, letting the user
 * decide which apps are Risky / Productive / Neutral / Other.
 */
class CategoryOverrides(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** The user-chosen kind for [packageName], or null if not overridden. */
    fun kindFor(packageName: String): CategoryKind? =
        prefs.getString(packageName, null)?.let { stored ->
            runCatching { CategoryKind.valueOf(stored) }.getOrNull()
        }

    fun setKind(packageName: String, kind: CategoryKind) {
        prefs.edit().putString(packageName, kind.name).apply()
    }

    /** Removes an override so the app reverts to its default categorization. */
    fun clear(packageName: String) {
        prefs.edit().remove(packageName).apply()
    }

    private companion object {
        const val PREFS_NAME = "category_overrides"
    }
}
