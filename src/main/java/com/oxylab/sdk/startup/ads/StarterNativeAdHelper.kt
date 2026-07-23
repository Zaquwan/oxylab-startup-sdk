package com.oxylab.sdk.startup.ads

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.nativead.AdChoicesView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.facebook.shimmer.ShimmerFrameLayout
import com.oxylab.sdk.startup.utils.StarterNetworkMonitor

/**
 * Extracted 1:1 from NativeAdHelper.
 * Dependencies replaced:
 *   AdConfig.isAdEnabled()              → configProvider.isAdEnabled()
 *   AdsManager.prepareAdContainer()     → adsManager.prepareAdContainer()
 *   AdsManager.hideAdContainer()        → adsManager.hideAdContainer()
 *   NetworkMonitor(context).isOnline()  → networkMonitor.isCurrentlyOnline()
 *   R.layout.* / R.id.*                → layoutConfig (constructor param)
 *
 * @param layoutConfig Resource IDs for all native ad layouts, shimmer layouts, and view IDs.
 */
class StarterNativeAdHelper(
    private val context: Context,
    private val configProvider: AdConfigProvider,
    private val adsManager: StarterAdsManager,
    private val networkMonitor: StarterNetworkMonitor,
    private val layoutConfig: NativeAdLayoutConfig
) {

    private companion object {
        const val TAG = "NativeAdHelper"
    }

    private lateinit var adLoader: AdLoader
    private val activeNativeAds = java.util.concurrent.ConcurrentHashMap<ViewGroup, NativeAd>()

    /** Destroys the NativeAd associated with the given container and clears all child views. */
    fun destroyAd(adContainer: ViewGroup) {
        activeNativeAds.remove(adContainer)?.destroy()
        adContainer.removeAllViews()
    }

    /** Destroys all active NativeAd instances managed by this helper. */
    fun destroyAll() {
        activeNativeAds.values.forEach { it.destroy() }
        activeNativeAds.clear()
    }

    private fun showShimmer(adContainer: ViewGroup, shimmerLayoutId: Int): View {
        val existingShimmer = adContainer.findViewById<ShimmerFrameLayout>(layoutConfig.shimmerContainerId)
        if (existingShimmer != null && adContainer.childCount == 1) {
            existingShimmer.startShimmer()
            return adContainer.getChildAt(0)
        }
        val shimmerView = LayoutInflater.from(context)
            .inflate(shimmerLayoutId, adContainer, false)
        adContainer.removeAllViews()
        adContainer.addView(shimmerView)
        shimmerView.findViewById<ShimmerFrameLayout>(layoutConfig.shimmerContainerId)?.startShimmer()
        return shimmerView
    }

    // ── Populate methods (bind NativeAd data to views) ──────────────────

    fun populateNativeAdViewLayout01(nativeAd: NativeAd, adView: NativeAdView) {
        adView.headlineView = adView.findViewById(layoutConfig.headlineId)
        adView.mediaView = adView.findViewById(layoutConfig.mediaViewId)
        adView.bodyView = adView.findViewById(layoutConfig.bodyId)
        adView.callToActionView = adView.findViewById(layoutConfig.callToActionId)
        adView.iconView = adView.findViewById(layoutConfig.iconId)
        adView.adChoicesView = adView.findViewById(layoutConfig.adChoicesId)
        populateCommonFields(nativeAd, adView, hasIcon = true, hasMedia = true)
    }

    fun populateNativeAdViewLayout02(nativeAd: NativeAd, adView: NativeAdView) {
        adView.headlineView = adView.findViewById(layoutConfig.headlineId)
        adView.mediaView = adView.findViewById(layoutConfig.mediaViewId)
        adView.bodyView = adView.findViewById(layoutConfig.bodyId)
        adView.callToActionView = adView.findViewById(layoutConfig.callToActionId)
        adView.adChoicesView = adView.findViewById(layoutConfig.adChoicesId)
        populateCommonFields(nativeAd, adView, hasIcon = false, hasMedia = true)
    }

    fun populateNativeAdViewLayout03(nativeAd: NativeAd, adView: NativeAdView) {
        adView.headlineView = adView.findViewById(layoutConfig.headlineId)
        adView.bodyView = adView.findViewById(layoutConfig.bodyId)
        adView.callToActionView = adView.findViewById(layoutConfig.callToActionId)
        adView.iconView = adView.findViewById(layoutConfig.iconId)
        adView.adChoicesView = adView.findViewById(layoutConfig.adChoicesId)
        populateCommonFields(nativeAd, adView, hasIcon = true, hasMedia = false)
    }

    fun populateNativeAdViewLayout04(nativeAd: NativeAd, adView: NativeAdView) {
        adView.headlineView = adView.findViewById(layoutConfig.headlineId)
        adView.mediaView = adView.findViewById(layoutConfig.mediaViewId)
        adView.bodyView = adView.findViewById(layoutConfig.bodyId)
        adView.callToActionView = adView.findViewById(layoutConfig.callToActionId)
        adView.iconView = adView.findViewById(layoutConfig.iconId)
        adView.adChoicesView = adView.findViewById(layoutConfig.adChoicesId)
        populateCommonFields(nativeAd, adView, hasIcon = true, hasMedia = true)
    }

    fun populateNativeAdViewFull(nativeAd: NativeAd, adView: NativeAdView) {
        adView.headlineView = adView.findViewById(layoutConfig.headlineId)
        adView.mediaView = adView.findViewById(layoutConfig.mediaViewId)
        adView.bodyView = adView.findViewById(layoutConfig.bodyId)
        adView.callToActionView = adView.findViewById(layoutConfig.callToActionId)
        adView.iconView = adView.findViewById(layoutConfig.iconId)
        adView.advertiserView = adView.findViewById(layoutConfig.advertiserId)
        adView.storeView = adView.findViewById(layoutConfig.storeId)
        adView.priceView = adView.findViewById(layoutConfig.priceId)
        adView.adChoicesView = adView.findViewById(layoutConfig.adChoicesId)

        populateCommonFields(nativeAd, adView, hasIcon = true, hasMedia = true)

        if (nativeAd.advertiser != null) {
            (adView.advertiserView as TextView).text = nativeAd.advertiser
            adView.advertiserView?.visibility = TextView.VISIBLE
        } else {
            adView.advertiserView?.visibility = TextView.GONE
        }
        if (nativeAd.store != null) {
            (adView.storeView as TextView).text = nativeAd.store
            adView.storeView?.visibility = TextView.VISIBLE
        } else {
            adView.storeView?.visibility = TextView.GONE
        }
        if (nativeAd.price != null) {
            (adView.priceView as TextView).text = nativeAd.price
            adView.priceView?.visibility = TextView.VISIBLE
        } else {
            adView.priceView?.visibility = TextView.GONE
        }
    }

    /** Shared populate logic to avoid duplication across layout variants. */
    private fun populateCommonFields(nativeAd: NativeAd, adView: NativeAdView, hasIcon: Boolean, hasMedia: Boolean) {
        (adView.headlineView as TextView).text = nativeAd.headline
        if (hasMedia) adView.mediaView?.setMediaContent(nativeAd.mediaContent)

        if (nativeAd.body != null) {
            (adView.bodyView as TextView).text = nativeAd.body
            adView.bodyView?.visibility = TextView.VISIBLE
        } else {
            adView.bodyView?.visibility = TextView.GONE
        }
        if (nativeAd.callToAction != null) {
            (adView.callToActionView as Button).text = nativeAd.callToAction
        }
        if (hasIcon) {
            if (nativeAd.icon != null) {
                (adView.iconView as ImageView).setImageDrawable(nativeAd.icon!!.drawable)
                adView.iconView?.visibility = ImageView.VISIBLE
            } else {
                adView.iconView?.visibility = ImageView.GONE
            }
        }
        if (nativeAd.adChoicesInfo != null) {
            adView.adChoicesView?.visibility = AdChoicesView.VISIBLE
        } else {
            adView.adChoicesView?.visibility = AdChoicesView.GONE
        }
        adView.setNativeAd(nativeAd)
    }

    // ── Load methods ────────────────────────────────────────────────────

    @JvmOverloads
    fun loadNativeAdWithLayout01(
        adUnitID: String, adContainer: ViewGroup, adVarName: String = "NATIVE",
        onLoaded: (() -> Unit)? = null, onFailed: (() -> Unit)? = null
    ) {
        loadNativeAdInternal(adUnitID, adContainer, adVarName,
            layoutConfig.shimmer01, layoutConfig.nativeLayout01,
            { nativeAd, adView -> populateNativeAdViewLayout01(nativeAd, adView) },
            onLoaded, onFailed
        )
    }

    fun loadNativeAdWithLayout02(adUnitID: String, adContainer: ViewGroup, adVarName: String = "NATIVE") {
        loadNativeAdInternal(adUnitID, adContainer, adVarName,
            layoutConfig.shimmer02, layoutConfig.nativeLayout02,
            { nativeAd, adView -> populateNativeAdViewLayout02(nativeAd, adView) }
        )
    }

    fun loadNativeAdWithLayout03(adUnitID: String, adContainer: ViewGroup, adVarName: String = "NATIVE") {
        loadNativeAdInternal(adUnitID, adContainer, adVarName,
            layoutConfig.shimmer03, layoutConfig.nativeLayout03,
            { nativeAd, adView -> populateNativeAdViewLayout03(nativeAd, adView) }
        )
    }

    fun loadNativeAdWithLayout04(adUnitID: String, adContainer: ViewGroup, adVarName: String = "NATIVE") {
        loadNativeAdInternal(adUnitID, adContainer, adVarName,
            layoutConfig.shimmer04, layoutConfig.nativeLayout04,
            { nativeAd, adView -> populateNativeAdViewLayout04(nativeAd, adView) }
        )
    }

    fun loadNativeAdFull(adUnitID: String, adContainer: ViewGroup, adVarName: String = "NATIVE") {
        loadNativeAdInternal(adUnitID, adContainer, adVarName,
            layoutConfig.shimmer01, layoutConfig.nativeLayoutFull,
            { nativeAd, adView -> populateNativeAdViewFull(nativeAd, adView) }
        )
    }

    fun loadNativeAd(adUnitID: String, adVarName: String = "NATIVE", onLoaded: (NativeAd) -> Unit, onFailed: () -> Unit) {
        if (!configProvider.isAdEnabled(adVarName)) {
            Log.d(TAG, "$adVarName load SKIPPED: disabled by specific flag")
            onFailed()
            return
        }
        if (!configProvider.isNativeEnabled()) {
            Log.d(TAG, "$adVarName load SKIPPED: disabled")
            onFailed()
            return
        }
        adLoader = AdLoader.Builder(context, adUnitID)
            .forNativeAd { nativeAd ->
                Log.d(TAG, "$adVarName : $adUnitID Shown")
                onLoaded(nativeAd)
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)
                    Log.e(TAG, "$adVarName Failed: ${error.message} (Code: ${error.code}) | ID: $adUnitID")
                    onFailed()
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }

    /**
     * Internal load method used by all loadNativeAdWithLayoutXX variants.
     * Preserves the exact pre-check → shimmer → load → populate → error flow.
     */
    private fun loadNativeAdInternal(
        adUnitID: String, adContainer: ViewGroup, adVarName: String,
        shimmerLayoutId: Int, nativeLayoutId: Int,
        populateFn: (NativeAd, NativeAdView) -> Unit,
        onLoaded: (() -> Unit)? = null, onFailed: (() -> Unit)? = null
    ) {
        if (!networkMonitor.isCurrentlyOnline()) {
            destroyAd(adContainer)
            adsManager.hideAdContainer(adContainer)
            onFailed?.invoke()
            return
        }
        if (!configProvider.isAdEnabled(adVarName)) {
            Log.d(TAG, "$adVarName load SKIPPED: disabled by specific flag")
            destroyAd(adContainer)
            adsManager.hideAdContainer(adContainer)
            onFailed?.invoke()
            return
        }
        if (!adsManager.prepareAdContainer(adContainer, StarterAdsManager.AdType.NATIVE)) {
            destroyAd(adContainer)
            onFailed?.invoke()
            return
        }
        showShimmer(adContainer, shimmerLayoutId)
        adLoader = AdLoader.Builder(context, adUnitID)
            .forNativeAd { nativeAd ->
                Log.d(TAG, "$adVarName : $adUnitID Shown")
                val adView = LayoutInflater.from(context)
                    .inflate(nativeLayoutId, adContainer, false) as NativeAdView
                populateFn(nativeAd, adView)
                destroyAd(adContainer)
                activeNativeAds[adContainer] = nativeAd
                adContainer.addView(adView)
                onLoaded?.invoke()
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    super.onAdFailedToLoad(error)
                    Log.e(TAG, "$adVarName Failed: ${error.message} (Code: ${error.code}) | ID: $adUnitID")
                    destroyAd(adContainer)
                    adsManager.hideAdContainer(adContainer)
                    onFailed?.invoke()
                }
            })
            .build()

        adLoader.loadAd(AdRequest.Builder().build())
    }
}
