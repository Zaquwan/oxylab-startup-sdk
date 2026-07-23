package com.oxylab.sdk.startup.core

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import java.util.concurrent.ConcurrentHashMap

/**
 * Firebase Remote Config implementation of [OxylabConfig].
 * 
 * Fetches Remote Config values once at initialization and caches them in memory for fast,
 * zero-overhead lookups throughout the app session.
 * 
 * Firebase Remote Config Dashboard String Rules:
 * - "1" or "true" = Enabled
 * - "0" or "false" = Disabled
 * - Unset / missing = Default (enabled)
 * 
 * Individual ad placement check:
 * Pass the exact [adVarName] string (e.g., "inter_splash", "native_home", "banner_main").
 */
open class FirebaseOxylabConfig @JvmOverloads constructor(
    private val remoteConfig: FirebaseRemoteConfig = FirebaseRemoteConfig.getInstance(),
    private val defaultEnabled: Boolean = true,
    private val defaultIntervalMs: Long = 40_000L,
    defaultsMap: Map<String, Any> = emptyMap()
) : OxylabConfig {

    private val cache = ConcurrentHashMap<String, String>()

    init {
        if (defaultsMap.isNotEmpty()) {
            try {
                remoteConfig.setDefaultsAsync(defaultsMap)
            } catch (_: Exception) {
                // Ignore if Remote Config is uninitialized in unit test environments
            }
        }
        syncCache()
    }

    /**
     * Configures minimum fetch interval in seconds.
     * Use 0L during development/testing for instant Remote Config updates,
     * or default 3600L (1 hour) in production.
     */
    fun setFetchInterval(intervalSeconds: Long) {
        try {
            val settings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(intervalSeconds)
                .build()
            remoteConfig.setConfigSettingsAsync(settings)
        } catch (_: Exception) {
            // Safe fallback
        }
    }

    /**
     * Call once during app or SDK initialization to fetch and activate Remote Config values
     * into memory for the current session.
     */
    fun fetchAndActivate(onComplete: ((Boolean) -> Unit)? = null) {
        remoteConfig.fetchAndActivate().addOnCompleteListener { task ->
            syncCache()
            onComplete?.invoke(task.isSuccessful)
        }
    }

    /**
     * Synchronizes currently activated Remote Config values into the in-memory session cache.
     */
    fun syncCache() {
        try {
            remoteConfig.all.forEach { (key, value) ->
                val str = value.asString()
                if (str.isNotEmpty()) {
                    cache[key] = str
                }
            }
        } catch (_: Exception) {
            // Ignore if Firebase Remote Config is not initialized yet
        }
    }

    override fun isGlobalAdsEnabled(): Boolean = isEnabled(KEY_GLOBAL_ADS)
    override fun isInterstitialEnabled(): Boolean = isEnabled(KEY_INTERSTITIAL_ADS)
    override fun isNativeEnabled(): Boolean = isEnabled(KEY_NATIVE_ADS)
    override fun isBannerEnabled(): Boolean = isEnabled(KEY_BANNER_ADS)
    override fun isAppOpenEnabled(): Boolean = isEnabled(KEY_APP_OPEN_ADS)
    override fun isRewardedEnabled(): Boolean = isEnabled(KEY_REWARDED_ADS)

    override fun isAdEnabled(adVarName: String): Boolean = isEnabled(adVarName)

    override fun getInterstitialInterval(): Long {
        return cache[KEY_INTERSTITIAL_INTERVAL]?.toLongOrNull()
            ?: remoteConfig.getString(KEY_INTERSTITIAL_INTERVAL).toLongOrNull()
            ?: defaultIntervalMs
    }

    override fun isInterstitialCooldownBypassed(adVarName: String): Boolean {
        if (adVarName.isEmpty()) return false
        val bypassKey = "bypass_$adVarName"
        val valStr = cache[bypassKey] ?: remoteConfig.getString(bypassKey)
        return valStr == "1" || valStr.equals("true", ignoreCase = true)
    }

    override fun isDebugLoggingEnabled(): Boolean {
        val valStr = cache[KEY_DEBUG_LOGGING] ?: remoteConfig.getString(KEY_DEBUG_LOGGING)
        return valStr == "1" || valStr.equals("true", ignoreCase = true)
    }

    private fun isEnabled(key: String): Boolean {
        if (key.isEmpty()) return true
        var valStr = cache[key]
        if (valStr == null) {
            valStr = remoteConfig.getString(key)
            if (valStr.isNotEmpty()) {
                cache[key] = valStr
            }
        }
        return when {
            valStr == "0" || valStr.equals("false", ignoreCase = true) -> false
            valStr == "1" || valStr.equals("true", ignoreCase = true) -> true
            else -> defaultEnabled
        }
    }

    companion object {
        const val KEY_GLOBAL_ADS = "global_ads"
        const val KEY_INTERSTITIAL_ADS = "interstitial_ads"
        const val KEY_NATIVE_ADS = "native_ads"
        const val KEY_BANNER_ADS = "banner_ads"
        const val KEY_APP_OPEN_ADS = "app_open_ads"
        const val KEY_REWARDED_ADS = "rewarded_ads"
        const val KEY_INTERSTITIAL_INTERVAL = "interstitial_interval"
        const val KEY_DEBUG_LOGGING = "debug_logging"
    }
}
