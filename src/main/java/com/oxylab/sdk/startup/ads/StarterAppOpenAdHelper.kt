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
    private var appOpenAdVarName: String = "APP_OPEN"
    private val excludedActivities = mutableSetOf<Class<out Activity>>()

    /** Exclude specific activities from showing the app open ad on resume */
    fun excludeActivity(vararg activityClasses: Class<out Activity>) {
        excludedActivities.addAll(activityClasses)
    }

    /** Include specific activities back if they were previously excluded */
    fun includeActivity(vararg activityClasses: Class<out Activity>) {
        excludedActivities.removeAll(activityClasses.toSet())
    }

    init {
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }

    /** Set the App Open Ad Unit ID and optional Remote Config variable name. */
    @JvmOverloads
    fun setAdUnitId(adUnitId: String, adVarName: String = "APP_OPEN") {
        this.appOpenAdUnitId = adUnitId
        this.appOpenAdVarName = adVarName
    }

    private fun isAdAvailable(): Boolean {
        if (appOpenAd != null && System.currentTimeMillis() - loadTime >= 4 * 60 * 60 * 1000) {
            appOpenAd = null
        }
        return appOpenAd != null
    }

    @JvmOverloads
    fun loadAppOpenAd(adVarName: String = appOpenAdVarName) {
        val unitId = appOpenAdUnitId ?: return
        if (isLoadingAd || isAdAvailable() || !networkMonitor.isCurrentlyOnline() || !configProvider.isGlobalAdsEnabled() || !configProvider.isAppOpenEnabled() || !configProvider.isAdEnabled(adVarName)) {
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
        if (excludedActivities.contains(activity.javaClass)) return
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
