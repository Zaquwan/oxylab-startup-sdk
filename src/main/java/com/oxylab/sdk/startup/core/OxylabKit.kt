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
        
    private var isInitialized = false

    /**
     * Initializes the SDK.
     * 
     * @param context Application context.
     * @param sdkConfig Configuration options for ads and timing.
     */
    fun initialize(context: Application, sdkConfig: OxylabConfig = DefaultOxylabConfig()) {
        if (isInitialized) return
        
        config = sdkConfig
        adsManager = StarterAdsManager(config)
        adsManager.initialize(context)
        isInitialized = true
    }
}
