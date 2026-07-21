package com.oxylab.sdk.startup.ads

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.oxylab.sdk.startup.R
import com.oxylab.sdk.startup.utils.StarterNetworkMonitor

class StarterBannerAdHelper(
    private val configProvider: AdConfigProvider,
    private val networkMonitor: StarterNetworkMonitor
) {
    private companion object {
        const val TAG = "StarterBannerAdHelper"
    }

    fun showCollapsibleBanner(activity: Activity, container: FrameLayout, adUnitID: String) {
        if (!networkMonitor.isCurrentlyOnline() || !configProvider.isGlobalAdsEnabled() || !configProvider.isBannerEnabled()) {
            return
        }

        if (container.visibility == View.VISIBLE && container.childCount > 0) return

        val bannerView = LayoutInflater.from(activity)
            .inflate(R.layout.collapsible_banner_layout, container, false)

        val adHolder = bannerView.findViewById<FrameLayout>(R.id.ad_holder)
        val closeBtn = bannerView.findViewById<ImageView>(R.id.btn_close_banner)

        if (adHolder == null || closeBtn == null) {
            Log.e(TAG, "Invalid collapsible banner layout!")
            return
        }

        val adView = AdView(activity)
        adView.adUnitId = adUnitID
        
        val displayMetrics = activity.resources.displayMetrics
        val widthPixels = displayMetrics.widthPixels
        val density = displayMetrics.density
        val adWidth = (widthPixels / density).toInt()
        val adSize = AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
        adView.setAdSize(adSize)

        adHolder.removeAllViews()
        adHolder.addView(adView)

        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                Log.d(TAG, "Collapsible Banner loaded successfully.")
                activity.runOnUiThread {
                    container.removeAllViews()
                    container.addView(bannerView)

                    container.post {
                        container.translationY = container.height.toFloat()
                        container.visibility = View.VISIBLE
                        container.animate()
                            .translationY(0f)
                            .setDuration(300)
                            .start()
                    }

                    closeBtn.setOnClickListener {
                        container.animate()
                            .translationY(container.height.toFloat())
                            .setDuration(300)
                            .withEndAction {
                                container.visibility = View.GONE
                                container.removeAllViews()
                                adView.destroy()
                            }
                            .start()
                    }
                }
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                Log.e(TAG, "Collapsible Banner failed to load: ${error.message}")
                adView.destroy()
            }
        }

        val extras = android.os.Bundle()
        extras.putString("collapsible", "bottom")
        val adRequest = AdRequest.Builder()
            .addNetworkExtrasBundle(com.google.ads.mediation.admob.AdMobAdapter::class.java, extras)
            .build()
        adView.loadAd(adRequest)
    }
}
