package com.oxylab.sdk.startup.core

import com.oxylab.sdk.startup.ads.AdConfigProvider

/**
 * Global configuration for the Oxylab SDK.
 * 
 * Implement this interface to provide dynamic values from your Remote Config provider,
 * or use [DefaultOxylabConfig] if you prefer static values.
 */
interface OxylabConfig : AdConfigProvider

/**
 * A default static implementation of [OxylabConfig].
 */
open class DefaultOxylabConfig(
    private val globalAds: Boolean = true,
    private val interstitialAds: Boolean = true,
    private val nativeAds: Boolean = true,
    private val bannerAds: Boolean = true,
    private val intervalMs: Long = 40_000L
) : OxylabConfig {
    override fun isGlobalAdsEnabled() = globalAds
    override fun isInterstitialEnabled() = interstitialAds
    override fun isNativeEnabled() = nativeAds
    override fun isBannerEnabled() = bannerAds
    override fun getInterstitialInterval() = intervalMs
    override fun isAdEnabled(adVarName: String) = true
}
