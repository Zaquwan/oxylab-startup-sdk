package com.oxylab.sdk.startup.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.oxylab.sdk.startup.utils.StarterNetworkMonitor

class StarterRewardedAdHelper(
    private val context: Context,
    private val configProvider: AdConfigProvider,
    private val networkMonitor: StarterNetworkMonitor
) {
    private companion object {
        const val TAG = "StarterRewardedAdHelper"
    }

    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false

    interface AdRewardListener {
        fun onRewardEarned()
    }

    fun loadRewardedAd(adUnitID: String) {
        if (!networkMonitor.isCurrentlyOnline() || !configProvider.isGlobalAdsEnabled() || !configProvider.isRewardedEnabled()) {
            return
        }
        if (rewardedAd != null || isRewardedLoading) {
            return
        }

        isRewardedLoading = true
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            context,
            adUnitID,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading = false
                    Log.d(TAG, "Rewarded ad loaded successfully.")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    isRewardedLoading = false
                    Log.e(TAG, "Rewarded ad failed to load: ${error.message}")
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, adUnitID: String, listener: AdRewardListener?) {
        if (rewardedAd == null) {
            Toast.makeText(activity, "Ad not ready, try again", Toast.LENGTH_SHORT).show()
            loadRewardedAd(adUnitID)
            return
        }

        rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                listener?.onRewardEarned()
                rewardedAd = null
                loadRewardedAd(adUnitID)
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                loadRewardedAd(adUnitID)
            }
        }

        rewardedAd?.show(activity) { rewardItem ->
            Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
        }
    }
}
