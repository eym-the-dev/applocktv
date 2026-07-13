package com.example

import android.content.Context
import android.content.SharedPreferences

object AppLockManager {
    private const val PREFS_NAME = "applock_prefs"
    private const val KEY_PIN = "lock_pin"
    private const val DEFAULT_PIN = "1234"

    // Set of default locked apps
    private val DEFAULT_LOCKED_APPS = setOf(
        "com.google.android.youtube",
        "com.google.android.youtube.kids"
    )

    private const val KEY_LOCKED_APPS = "locked_apps_list"

    // Temporary session state to store currently unlocked application package
    private var unlockedPackage: String? = null

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Gets the saved PIN or returns the default "1234".
     */
    fun getPin(context: Context): String {
        return getPrefs(context).getString(KEY_PIN, DEFAULT_PIN) ?: DEFAULT_PIN
    }

    /**
     * Saves a new 4-digit PIN.
     */
    fun savePin(context: Context, pin: String): Boolean {
        if (pin.length == 4 && pin.all { it.isDigit() }) {
            getPrefs(context).edit().putString(KEY_PIN, pin).apply()
            return true
        }
        return false
    }

    /**
     * Gets the list of locked packages.
     */
    fun getLockedPackages(context: Context): Set<String> {
        return getPrefs(context).getStringSet(KEY_LOCKED_APPS, DEFAULT_LOCKED_APPS) ?: DEFAULT_LOCKED_APPS
    }

    /**
     * Toggles lock status of a package name.
     */
    fun togglePackageLock(context: Context, packageName: String) {
        val current = getLockedPackages(context).toMutableSet()
        if (current.contains(packageName)) {
            current.remove(packageName)
        } else {
            current.add(packageName)
        }
        getPrefs(context).edit().putStringSet(KEY_LOCKED_APPS, current).apply()
    }

    /**
     * Checks if the package is locked and requires PIN verification.
     */
    fun isPackageLocked(context: Context, packageName: String): Boolean {
        // If it's already unlocked in this foreground session, don't lock
        if (packageName == unlockedPackage) {
            return false
        }
        return getLockedPackages(context).contains(packageName)
    }

    /**
     * Temporarily unlocks a package for the current session.
     */
    fun setSessionUnlocked(packageName: String?) {
        unlockedPackage = packageName
    }

    /**
     * Resets the session unlock state. Called when the user exits the locked package.
     */
    fun resetSessionUnlock() {
        unlockedPackage = null
    }

    /**
     * Gets currently unlocked package.
     */
    fun getUnlockedSessionPackage(): String? {
        return unlockedPackage
    }
}
