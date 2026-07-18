package com.oxylab.sdk.startup.ads

import android.content.Context
import android.util.Log
import android.view.View
import com.google.android.gms.ads.MobileAds

/**
 * Extracted from AdsManager. Handles MobileAds SDK initialization and
 * ad container visibility management.
 *
 * Uses [AdConfigProvider] instead of hardcoded AdConfig/RemoteConfig.
 */
class StarterAdsManager(private val configProvider: AdConfigProvider) {

    companion object {
        private const val TAG = "StarterAdsManager"
    }

    enum class AdType { BANNER, INTERSTITIAL, NATIVE }

    @Volatile
    private var isSdkInitialized = false

    @Volatile
    var isInterstitialShowing = false

    @Volatile
    var isAppOpenAdShowing = false

    fun initialize(context: Context) {
        if (!configProvider.isGlobalAdsEnabled()) {
            Log.d(TAG, "Ad SDK initialization SKIPPED: Global ads disabled")
            return
        }
        if (isSdkInitialized) {
            Log.d(TAG, "Ad SDK already initialized, skipping")
            return
        }
        try {
            Log.d(TAG, "Initializing Ad SDK...")
            MobileAds.initialize(context) { status ->
                isSdkInitialized = true
                Log.d(TAG, "Ad SDK initialized: ${status.adapterStatusMap}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Ad SDK initialization FAILED", e)
        }
    }

    fun prepareAdContainer(container: View, adType: AdType, shimmerView: View? = null): Boolean {
        val isEnabled = when (adType) {
            AdType.BANNER -> configProvider.isBannerEnabled()
            AdType.INTERSTITIAL -> configProvider.isInterstitialEnabled()
            AdType.NATIVE -> configProvider.isNativeEnabled()
        }
        if (!isEnabled) {
            container.visibility = View.GONE
            shimmerView?.visibility = View.GONE
            container.removeCallbacks(null)
            Log.d(TAG, "Ad container HIDDEN for type: $adType (disabled)")
            return false
        }
        return true
    }

    fun hideAdContainer(container: View) {
        container.visibility = View.GONE
    }
}
