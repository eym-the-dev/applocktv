package com.example

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AppLockAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent) {
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            val packageName = event.packageName?.toString() ?: return
            
            Log.d("AppLockService", "Foreground package changed to: $packageName")

            // 1. Check if they exited the currently unlocked package
            val sessionUnlockedApp = AppLockManager.getUnlockedSessionPackage()
            if (sessionUnlockedApp != null && packageName != this.packageName && packageName != sessionUnlockedApp) {
                Log.d("AppLockService", "Exited unlocked package $sessionUnlockedApp. Locking again.")
                AppLockManager.resetSessionUnlock()
            }

            // 2. Check if the package is locked and needs verification
            if (AppLockManager.isPackageLocked(this, packageName)) {
                Log.d("AppLockService", "Package is locked: $packageName. Showing PIN Lock screen.")
                
                val lockIntent = Intent(this, LockActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                    putExtra(LockActivity.EXTRA_LOCKED_PACKAGE, packageName)
                }
                startActivity(lockIntent)
            }
        }
    }

    override fun onInterrupt() {
        Log.d("AppLockService", "Service Interrupted")
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d("AppLockService", "Service Connected Successfully")
    }
}
