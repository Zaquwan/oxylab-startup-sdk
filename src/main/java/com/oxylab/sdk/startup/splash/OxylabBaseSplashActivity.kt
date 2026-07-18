package com.oxylab.sdk.startup.splash

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.firebase.auth.FirebaseAuth
import com.oxylab.sdk.startup.ads.AdConfigProvider
import com.oxylab.sdk.startup.ads.AdTimingProvider
import com.oxylab.sdk.startup.ads.NativeAdLayoutConfig
import com.oxylab.sdk.startup.ads.StarterAdsManager
import com.oxylab.sdk.startup.ads.StarterInterstitialAdHelper
import com.oxylab.sdk.startup.ads.StarterNativeAdHelper
import com.oxylab.sdk.startup.core.OxylabKit
import com.oxylab.sdk.startup.utils.StarterNetworkMonitor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout

/**
 * Plug-and-play Base Activity for the Splash Screen.
 * 
 * Hides all complex engine wiring, AdMob tracking, and Firebase Auth behind the scenes.
 * Simply extend this activity, provide your layout IDs, and you're done.
 */
abstract class OxylabBaseSplashActivity : AppCompatActivity() {

    // ── Developer Must Provide These ──

    /** Your custom layout XML for the splash screen (e.g. R.layout.activity_splash) */
    abstract fun getLayoutResId(): Int
    
    /** The ID of the FrameLayout where native ads will load (e.g. R.id.adContainer) */
    abstract fun getAdContainerId(): Int
    
    /** The ID of the parent layout wrapping your error/loading views (e.g. R.id.layoutError) */
    abstract fun getErrorLayoutId(): Int
    
    /** The ID of your title TextView in the error layout */
    abstract fun getTitleTextViewId(): Int
    
    /** The ID of your message TextView in the error layout */
    abstract fun getMessageTextViewId(): Int
    
    /** The ID of your ProgressBar in the error layout */
    abstract fun getProgressBarId(): Int
    
    /** The ID of your Retry Button in the error layout */
    abstract fun getRetryButtonId(): Int

    /** The Activity to start after the splash screen finishes */
    abstract fun getNextActivityClass(): Class<out Activity>

    /** Your AdMob Native Ad Unit ID for the splash screen */
    abstract fun getNativeAdUnitId(): String
    
    /** Your AdMob Interstitial Ad Unit ID for the splash screen */
    abstract fun getInterstitialAdUnitId(): String

    // ── Optional Overrides ──

    /** Minimum time in milliseconds the splash screen must stay visible. Default 8000L. */
    open fun getMinSplashTimeMs(): Long = 8000L
    
    /** Maximum time in milliseconds to wait for the splash screen to finish loading. */
    open fun getMaxWaitTimeMs(): Long = 12000L
    
    /** Maximum time to wait if the device is offline before proceeding anyway. */
    open fun getOfflineMaxWaitTimeMs(): Long = 5000L
    
    /** Interval to poll for readiness checks. */
    open fun getPollIntervalMs(): Long = 1000L
    
    /** Whether to force Dark Mode. Default true. */
    open fun isDarkModeEnabled(): Boolean = true

    // ── Internal State ──

    private val authState = MutableStateFlow<AuthStatus>(AuthStatus.Loading)
    private lateinit var networkMonitor: StarterNetworkMonitor
    private lateinit var nativeAdHelper: StarterNativeAdHelper
    private lateinit var interstitialAdHelper: StarterInterstitialAdHelper
    private var isProceeding = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        if (isDarkModeEnabled()) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        setContentView(getLayoutResId())

        findViewById<View>(getRetryButtonId())?.setOnClickListener {
            signInAnonymously()
        }

        networkMonitor = StarterNetworkMonitor(this)
        
        val configProvider = InternalAdConfig()
        val adsManager = StarterAdsManager(configProvider)
        adsManager.initialize(this)
        
        nativeAdHelper = StarterNativeAdHelper(
            this, configProvider, adsManager, networkMonitor,
            NativeAdLayoutConfig(
                nativeLayout01 = com.oxylab.sdk.startup.R.layout.native_ad_layout_01,
                nativeLayout02 = com.oxylab.sdk.startup.R.layout.native_ad_layout_02,
                nativeLayout03 = com.oxylab.sdk.startup.R.layout.native_ad_layout_03,
                nativeLayout04 = com.oxylab.sdk.startup.R.layout.native_ad_layout_04,
                nativeLayoutFull = com.oxylab.sdk.startup.R.layout.native_ad_layout_full,
                shimmer01 = com.oxylab.sdk.startup.R.layout.shimmer_native_ad_01,
                shimmer02 = com.oxylab.sdk.startup.R.layout.shimmer_native_ad_02,
                shimmer03 = com.oxylab.sdk.startup.R.layout.shimmer_native_ad_03,
                shimmer04 = com.oxylab.sdk.startup.R.layout.shimmer_native_ad_04,
                shimmerContainerId = com.oxylab.sdk.startup.R.id.shimmer_view_container,
                headlineId = com.oxylab.sdk.startup.R.id.native_ad_headline,
                mediaViewId = com.oxylab.sdk.startup.R.id.native_ad_media_view,
                bodyId = com.oxylab.sdk.startup.R.id.native_ad_body,
                callToActionId = com.oxylab.sdk.startup.R.id.native_ad_call_to_action,
                iconId = com.oxylab.sdk.startup.R.id.native_ad_icon,
                adChoicesId = com.oxylab.sdk.startup.R.id.native_ad_choice_view,
            )
        )
        
        interstitialAdHelper = StarterInterstitialAdHelper(
            this, configProvider, InternalAdTiming(), adsManager,
            android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth,
            com.oxylab.sdk.startup.R.layout.dialog_loading_ad
        )

        val adContainer = findViewById<ViewGroup>(getAdContainerId())
        if (adContainer != null) {
            nativeAdHelper.loadNativeAdWithLayout04(getNativeAdUnitId(), adContainer, "NATIVE_SPLASH")
        }
        interstitialAdHelper.loadAd(getInterstitialAdUnitId(), "INTER_SPLASH")

        startAuthObservation()
        signInAnonymously()
        startReadinessLoop()
    }

    private fun signInAnonymously() {
        val auth = FirebaseAuth.getInstance()
        if (auth.currentUser != null) {
            authState.value = AuthStatus.Authenticated
            return
        }
        
        lifecycleScope.launch {
            try {
                authState.value = AuthStatus.Loading
                withTimeout(5000) { auth.signInAnonymously().await() }
                authState.value = AuthStatus.Authenticated
            } catch (e: Exception) {
                authState.value = AuthStatus.Error(e.message ?: "Failed")
            }
        }
    }

    private fun startAuthObservation() {
        lifecycleScope.launch {
            authState.collect { status ->
                when (status) {
                    AuthStatus.Authenticated -> hideOverlay()
                    AuthStatus.NoInternet -> showNoInternet("No Internet", "Please check connection")
                    is AuthStatus.Error -> showError("Something went wrong", status.message)
                    AuthStatus.Loading -> showLoading("Loading...")
                }
            }
        }
    }

    private fun startReadinessLoop() {
        var startTime = System.currentTimeMillis()

        lifecycleScope.launch {
            var elapsed = 0L
            var wasOffline = !networkMonitor.isCurrentlyOnline()

            while (elapsed < getMaxWaitTimeMs()) {
                val isOnline = networkMonitor.isCurrentlyOnline()

                if (!isOnline) {
                    wasOffline = true
                    signInAnonymously()
                    if (elapsed >= getOfflineMaxWaitTimeMs()) {
                        break
                    }
                } else {
                    if (wasOffline) {
                        wasOffline = false
                        startTime = System.currentTimeMillis()
                        
                        val adContainer = findViewById<ViewGroup>(getAdContainerId())
                        if (adContainer != null) {
                            nativeAdHelper.loadNativeAdWithLayout04(getNativeAdUnitId(), adContainer, "NATIVE_SPLASH")
                        }
                        interstitialAdHelper.loadAd(getInterstitialAdUnitId(), "INTER_SPLASH")
                    }
                }

                val isAuthenticated = authState.value is AuthStatus.Authenticated
                if (isAuthenticated) {
                    val adContainer = findViewById<ViewGroup>(getAdContainerId())
                    val isNativeShown = adContainer != null && adContainer.childCount > 0 && adContainer.getChildAt(0) is NativeAdView
                    val isInterLoaded = interstitialAdHelper.isAdLoaded()

                    if (isNativeShown && isInterLoaded) {
                        break
                    }
                }

                delay(getPollIntervalMs())
                elapsed = System.currentTimeMillis() - startTime
            }
            proceedToApp()
        }
    }

    private fun proceedToApp() {
        if (isProceeding) return
        isProceeding = true
        interstitialAdHelper.showAd(ignoreInterval = true) {
            startActivity(Intent(this, getNextActivityClass()))
            @Suppress("DEPRECATION")
            overridePendingTransition(com.oxylab.sdk.startup.R.anim.fade_in, com.oxylab.sdk.startup.R.anim.fade_out)
            finish()
        }
    }

    // ── UI Helpers ──

    private fun showNoInternet(title: String, message: String) {
        findViewById<View>(getErrorLayoutId())?.visibility = View.VISIBLE
        findViewById<TextView>(getTitleTextViewId())?.text = title
        findViewById<TextView>(getMessageTextViewId())?.text = message
        findViewById<View>(getProgressBarId())?.visibility = View.GONE
        findViewById<View>(getRetryButtonId())?.visibility = View.VISIBLE
    }

    private fun showError(title: String, message: String) {
        findViewById<View>(getErrorLayoutId())?.visibility = View.VISIBLE
        findViewById<TextView>(getTitleTextViewId())?.text = title
        findViewById<TextView>(getMessageTextViewId())?.text = message
        findViewById<View>(getProgressBarId())?.visibility = View.GONE
        findViewById<View>(getRetryButtonId())?.visibility = View.VISIBLE
    }

    private fun showLoading(title: String) {
        findViewById<View>(getErrorLayoutId())?.visibility = View.VISIBLE
        findViewById<TextView>(getTitleTextViewId())?.text = title
        findViewById<TextView>(getMessageTextViewId())?.text = ""
        findViewById<View>(getProgressBarId())?.visibility = View.VISIBLE
        findViewById<View>(getRetryButtonId())?.visibility = View.GONE
    }

    private fun hideOverlay() {
        findViewById<View>(getErrorLayoutId())?.visibility = View.GONE
    }

    // ── Internal Classes ──

    private sealed class AuthStatus {
        object Loading : AuthStatus()
        object Authenticated : AuthStatus()
        object NoInternet : AuthStatus()
        data class Error(val message: String) : AuthStatus()
    }

    private inner class InternalAdConfig : AdConfigProvider {
        override fun isGlobalAdsEnabled() = OxylabKit.config.isGlobalAdsEnabled()
        override fun isInterstitialEnabled() = OxylabKit.config.isInterstitialEnabled()
        override fun isNativeEnabled() = OxylabKit.config.isNativeEnabled()
        override fun isBannerEnabled() = OxylabKit.config.isBannerEnabled()
        override fun isAdEnabled(adVarName: String) = true
        override fun getInterstitialInterval() = OxylabKit.config.getInterstitialIntervalMs()
    }

    private inner class InternalAdTiming : AdTimingProvider {
        override fun getLastAdTime(c: Context) = c.getSharedPreferences("oxylab_ads", 0).getLong("last_ad", 0L)
        override fun updateLastAdTime(c: Context) {
            c.getSharedPreferences("oxylab_ads", 0).edit().putLong("last_ad", System.currentTimeMillis()).apply()
        }
    }
}
