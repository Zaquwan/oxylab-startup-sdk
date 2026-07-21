package com.oxylab.sdk.startup.ads

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.oxylab.sdk.startup.utils.StarterNetworkMonitor

class StarterAppOpenAdHelper(
    private val application: Application,
    private val configProvider: AdConfigProvider,
    private val adsManager: StarterAdsManager,
    private val networkMonitor: StarterNetworkMonitor
) : Application.ActivityLifecycleCallbacks, DefaultLifecycleObserver {

    private companion object {
        const val TAG = "StarterAppOpenAdHelper"
    }

    private var appOpenAd: AppOpenAd? = null
    private var isLoadingAd = false
    private var isShowingAd = false
    private var loadTime = 0L
    private var currentActivity: Activity? = null
    
    private var appOpenAdUnitId: String? = null

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /** Set the App Open Ad Unit ID. If not set, app open ads will not load. */
    fun setAdUnitId(adUnitId: String) {
        this.appOpenAdUnitId = adUnitId
    }

    private fun isAdAvailable(): Boolean {
        return appOpenAd != null && (System.currentTimeMillis() - loadTime < 4 * 60 * 60 * 1000)
    }

    fun loadAppOpenAd() {
        val unitId = appOpenAdUnitId ?: return
        if (isLoadingAd || isAdAvailable() || !networkMonitor.isCurrentlyOnline() || !configProvider.isGlobalAdsEnabled() || !configProvider.isAppOpenEnabled()) {
            return
        }

        isLoadingAd = true
        val request = AdRequest.Builder().build()
        AppOpenAd.load(
            application,
            unitId,
            request,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    appOpenAd = ad
                    isLoadingAd = false
                    loadTime = System.currentTimeMillis()
                    Log.d(TAG, "AppOpenAd loaded successfully.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    isLoadingAd = false
                    Log.e(TAG, "AppOpenAd failed to load: ${error.message}")
                }
            }
        )
    }

    fun showAppOpenAdIfAvailable(activity: Activity) {
        if (isShowingAd || adsManager.isInterstitialShowing) return
        if (!isAdAvailable()) {
            loadAppOpenAd()
            return
        }

        isShowingAd = true
        adsManager.isAppOpenAdShowing = true
        appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                appOpenAd = null
                isShowingAd = false
                adsManager.isAppOpenAdShowing = false
                loadAppOpenAd()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                appOpenAd = null
                isShowingAd = false
                adsManager.isAppOpenAdShowing = false
                loadAppOpenAd()
            }
        }
        appOpenAd?.show(activity)
    }

    override fun onStart(owner: LifecycleOwner) {
        super.onStart(owner)
        currentActivity?.let { showAppOpenAdIfAvailable(it) }
    }

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityStarted(activity: Activity) { currentActivity = activity }
    override fun onActivityResumed(activity: Activity) { currentActivity = activity }
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivityStopped(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) { currentActivity = null }
}
