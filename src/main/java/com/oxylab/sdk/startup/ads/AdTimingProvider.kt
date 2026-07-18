package com.oxylab.sdk.startup.ads

import android.content.Context

/**
 * Interface for tracking interstitial ad timing.
 * Replaces direct dependency on ThemeManager.getLastAdTime() / updateLastAdTime().
 */
interface AdTimingProvider {
    fun getLastAdTime(context: Context): Long
    fun updateLastAdTime(context: Context)
}
