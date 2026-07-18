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

open class DefaultAdTimingProvider : AdTimingProvider {
    override fun getLastAdTime(context: Context): Long {
        return context.getSharedPreferences("oxylab_ads_prefs", Context.MODE_PRIVATE)
            .getLong("last_ad_time", 0L)
    }

    override fun updateLastAdTime(context: Context) {
        context.getSharedPreferences("oxylab_ads_prefs", Context.MODE_PRIVATE)
            .edit().putLong("last_ad_time", System.currentTimeMillis()).apply()
    }
}
