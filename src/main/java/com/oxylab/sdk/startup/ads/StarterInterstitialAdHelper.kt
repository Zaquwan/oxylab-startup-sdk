package com.oxylab.sdk.startup.ads

import android.app.Activity
import android.app.Dialog
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Extracted 1:1 from InterstitialAdHelper.
 * Dependencies replaced:
 *   AdConfig.isAdEnabled()       → configProvider.isAdEnabled()
 *   AdsManager.isInterstitialEnabled() → configProvider.isInterstitialEnabled()
 *   AdsManager.isInterstitialShowing   → adsManager.isInterstitialShowing
 *   ThemeManager.getLastAdTime()       → timingProvider.getLastAdTime()
 *   ThemeManager.updateLastAdTime()    → timingProvider.updateLastAdTime()
 *   R.style.AdLoadingDialogTheme       → dialogStyleResId (constructor param)
 *   R.layout.dialog_loading_ad         → dialogLayoutResId (constructor param)
 *
 * @param dialogStyleResId  Style resource for the loading dialog (was R.style.AdLoadingDialogTheme)
 * @param dialogLayoutResId Layout resource for the loading dialog (was R.layout.dialog_loading_ad)
 */
class StarterInterstitialAdHelper(
    private val activity: Activity,
    private val configProvider: AdConfigProvider,
    private val timingProvider: AdTimingProvider,
    private val adsManager: StarterAdsManager,
    private val dialogStyleResId: Int,
    private val dialogLayoutResId: Int
) {

    private companion object {
        const val TAG = "InterstitialAdHelper"
    }

    private var mInterstitialAd: InterstitialAd? = null
    private var isAdLoading = false
    private var currentAdVarName: String = "INTER"
    private var currentAdUnitId: String = ""

    @JvmOverloads
    fun loadAd(adUnitId: String, adVarName: String = "INTER") {
        if (!configProvider.isAdEnabled(adVarName)) {
            Log.d(TAG, "$adVarName load SKIPPED: disabled by specific flag")
            return
        }
        if (!configProvider.isInterstitialEnabled()) {
            Log.d(TAG, "$adVarName load SKIPPED: disabled")
            return
        }
        if (mInterstitialAd != null || isAdLoading) return

        currentAdVarName = adVarName
        currentAdUnitId = adUnitId
        isAdLoading = true
        val adRequest = AdRequest.Builder().build()

        InterstitialAd.load(activity, adUnitId, adRequest, object : InterstitialAdLoadCallback() {
            override fun onAdFailedToLoad(adError: LoadAdError) {
                Log.e(TAG, "$adVarName Failed: ${adError.message} | ID: $adUnitId")
                mInterstitialAd = null
                isAdLoading = false
            }

            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                Log.d(TAG, "$adVarName Loaded | ID: $adUnitId")
                mInterstitialAd = interstitialAd
                isAdLoading = false
            }
        })
    }

    @JvmOverloads
    fun showAd(ignoreInterval: Boolean = false, showLoadingDialog: Boolean = true, onAdDismissed: (() -> Unit)? = null) {
        if (!configProvider.isAdEnabled(currentAdVarName)) {
            onAdDismissed?.invoke()
            return
        }
        if (!configProvider.isInterstitialEnabled()) {
            onAdDismissed?.invoke()
            return
        }

        if (activity.isFinishing || activity.isDestroyed) {
            onAdDismissed?.invoke()
            return
        }

        if (!ignoreInterval) {
            val lastTime = timingProvider.getLastAdTime(activity)
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastTime < configProvider.getInterstitialInterval()) {
                onAdDismissed?.invoke()
                return
            }
        }

        if (isAdLoaded()) {
            if (showLoadingDialog) {
                val dialog = Dialog(activity, dialogStyleResId)
                dialog.setContentView(dialogLayoutResId)
                dialog.setCancelable(false)

                try {
                    dialog.show()
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to show loading dialog: ${e.message}")
                    showAdInternal(ignoreInterval, onAdDismissed, null)
                    return
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    if (!activity.isFinishing && !activity.isDestroyed) {
                        showAdInternal(ignoreInterval, onAdDismissed, dialog)
                    } else {
                        try {
                            if (dialog.isShowing) {
                                dialog.dismiss()
                            }
                        } catch (e: Exception) {}
                        onAdDismissed?.invoke()
                    }
                }, 2000)
            } else {
                showAdInternal(ignoreInterval, onAdDismissed, null)
            }
        } else {
            onAdDismissed?.invoke()
        }
    }

    private fun showAdInternal(ignoreInterval: Boolean, onAdDismissed: (() -> Unit)?, dialog: Dialog?) {
        if (activity.isFinishing || activity.isDestroyed) {
            try {
                if (dialog?.isShowing == true) dialog.dismiss()
            } catch (e: Exception) {}
            onAdDismissed?.invoke()
            return
        }
        if (isAdLoaded()) {
            var isCallbackTriggered = false
            mInterstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    isCallbackTriggered = true
                    Log.d(TAG, "$currentAdVarName : $currentAdUnitId Shown")
                    adsManager.isInterstitialShowing = true
                    try {
                        if (dialog?.isShowing == true) dialog.dismiss()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error dismissing dialog on show: ${e.message}")
                    }
                }

                override fun onAdDismissedFullScreenContent() {
                    isCallbackTriggered = true
                    mInterstitialAd = null
                    adsManager.isInterstitialShowing = false
                    try {
                        if (dialog?.isShowing == true) dialog.dismiss()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error dismissing dialog on dismiss: ${e.message}")
                    }
                    if (!ignoreInterval) {
                        timingProvider.updateLastAdTime(activity)
                    }
                    onAdDismissed?.invoke()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    isCallbackTriggered = true
                    mInterstitialAd = null
                    adsManager.isInterstitialShowing = false
                    try {
                        if (dialog?.isShowing == true) dialog.dismiss()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error dismissing dialog on fail: ${e.message}")
                    }
                    onAdDismissed?.invoke()
                }
            }

            // Safety timeout: if ad doesn't show in 5 seconds, dismiss dialog and skip ad
            Handler(Looper.getMainLooper()).postDelayed({
                if (!isCallbackTriggered && !activity.isFinishing && !activity.isDestroyed) {
                    Log.e(TAG, "Ad show timeout: dismissing dialog and skipping ad")
                    try {
                        if (dialog?.isShowing == true) dialog.dismiss()
                    } catch (e: Exception) {
                        Log.e(TAG, "Error dismissing dialog on timeout: ${e.message}")
                    }
                    onAdDismissed?.invoke()
                }
            }, 5000)

            mInterstitialAd?.show(activity)
        } else {
            try {
                if (dialog?.isShowing == true) dialog.dismiss()
            } catch (e: Exception) {}
            onAdDismissed?.invoke()
        }
    }

    fun isAdLoaded(): Boolean {
        return mInterstitialAd != null
    }
}
