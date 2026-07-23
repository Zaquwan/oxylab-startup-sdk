package com.oxylab.sdk.startup.core

import android.app.Application
import android.content.Context
import com.oxylab.sdk.startup.ads.StarterAdsManager

/**
 * Singleton entry point for the Oxylab SDK.
 * 
 * Apps should call [initialize] exactly once in their Application class.
 */
object OxylabKit {
    
    lateinit var config: OxylabConfig
        private set
        
    lateinit var adsManager: StarterAdsManager
        private set
        
    lateinit var appOpenAdHelper: com.oxylab.sdk.startup.ads.StarterAppOpenAdHelper
        private set
        
    lateinit var rewardedAdHelper: com.oxylab.sdk.startup.ads.StarterRewardedAdHelper
        private set
        
    lateinit var bannerAdHelper: com.oxylab.sdk.startup.ads.StarterBannerAdHelper
        private set
        
    var nativeAdLayoutConfig: com.oxylab.sdk.startup.ads.NativeAdLayoutConfig = com.oxylab.sdk.startup.ads.NativeAdLayoutConfig()
        
    @Volatile
    private var initialized = false

    val isInitialized: Boolean
        get() = initialized

    /**
     * Initializes the SDK.
     * 
     * @param context Application context.
     * @param sdkConfig Configuration options for ads and timing.
     * @param layoutConfig Custom layout configuration for native ads.
     */
    @JvmOverloads
    @Synchronized
    fun initialize(
        context: Application, 
        sdkConfig: OxylabConfig = DefaultOxylabConfig(),
        layoutConfig: com.oxylab.sdk.startup.ads.NativeAdLayoutConfig = com.oxylab.sdk.startup.ads.NativeAdLayoutConfig()
    ) {
        if (initialized) return
        
        config = sdkConfig
        nativeAdLayoutConfig = layoutConfig
        adsManager = StarterAdsManager(config)
        adsManager.initialize(context)
        
        val networkMonitor = com.oxylab.sdk.startup.utils.StarterNetworkMonitor(context)
        appOpenAdHelper = com.oxylab.sdk.startup.ads.StarterAppOpenAdHelper(context, config, adsManager, networkMonitor)
        rewardedAdHelper = com.oxylab.sdk.startup.ads.StarterRewardedAdHelper(context, config, networkMonitor)
        bannerAdHelper = com.oxylab.sdk.startup.ads.StarterBannerAdHelper(config, networkMonitor)
        
        initialized = true
    }
}
