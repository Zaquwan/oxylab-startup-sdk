package com.oxylab.sdk.startup.ads

/**
 * Interface for ad enable/disable checks.
 * Replaces direct dependency on AdConfig + AdsManager enable/disable methods.
 * App implements this using Firebase Remote Config, hardcoded values, or any source.
 */
interface AdConfigProvider {
    /** Global kill switch for all ads. */
    fun isGlobalAdsEnabled(): Boolean
    /** Whether interstitial ads are enabled globally. */
    fun isInterstitialEnabled(): Boolean
    /** Whether native ads are enabled globally. */
    fun isNativeEnabled(): Boolean
    /** Whether banner ads are enabled globally. */
    fun isBannerEnabled(): Boolean
    /** Whether a specific ad placement is enabled by its variable name. */
    fun isAdEnabled(adVarName: String): Boolean
    /** Interval in ms between two interstitial ads. */
    fun getInterstitialInterval(): Long
}
