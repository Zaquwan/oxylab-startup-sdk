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
import com.oxylab.sdk.startup.ads.StarterBannerAdHelper
import android.widget.FrameLayout
import com.google.android.gms.ads.AdView
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

    // ── Required — must be provided by the app ──

    /** The Activity to start after the splash screen finishes */
    abstract fun getNextActivityClass(): Class<out Activity>

    /** Your AdMob Native Ad Unit ID for the splash screen */
    abstract fun getNativeAdUnitId(): String

    /** Your AdMob Interstitial Ad Unit ID for the splash screen */
    abstract fun getInterstitialAdUnitId(): String

    enum class SplashAdType { NATIVE, BANNER, NONE }

    /** Choose which ad type to show on the splash screen. Default is NATIVE. */
    open fun getSplashAdType(): SplashAdType = SplashAdType.NATIVE

    /** Your AdMob Banner Ad Unit ID for the splash screen. Only used if getSplashAdType() is BANNER. */
    open fun getBannerAdUnitId(): String = ""

    // ── Optional Layout Overrides (SDK ships default layouts if you skip these) ──

    /**
     * Your custom layout XML for the splash screen.
     * Default: SDK built-in [R.layout.default_splash].
     */
    open fun getLayoutResId(): Int = com.oxylab.sdk.startup.R.layout.default_splash

    /**
     * The ID of the FrameLayout where native ads will load.
     * Default: [R.id.oxylab_splash_ad_container] from the SDK default layout.
     */
    open fun getAdContainerId(): Int = com.oxylab.sdk.startup.R.id.oxylab_splash_ad_container

    /**
     * The ID of the parent layout wrapping your error/loading views.
     * Default: [R.id.oxylab_splash_layout_error] from the SDK default layout.
     */
    open fun getErrorLayoutId(): Int = com.oxylab.sdk.startup.R.id.oxylab_splash_layout_error

    /**
     * The ID of your title TextView in the error layout.
     * Default: [R.id.oxylab_splash_tv_title] from the SDK default layout.
     */
    open fun getTitleTextViewId(): Int = com.oxylab.sdk.startup.R.id.oxylab_splash_tv_title

    /**
     * The ID of your message TextView in the error layout.
     * Default: [R.id.oxylab_splash_tv_message] from the SDK default layout.
     */
    open fun getMessageTextViewId(): Int = com.oxylab.sdk.startup.R.id.oxylab_splash_tv_message

    /**
     * The ID of your main loading indicator (ProgressBar or LottieAnimationView).
     * Default: [R.id.oxylab_splash_lottie] from the SDK default layout.
     */
    open fun getProgressBarId(): Int = com.oxylab.sdk.startup.R.id.oxylab_splash_lottie

    /**
     * The ID of your Retry Button in the error layout.
     * Default: [R.id.oxylab_splash_btn_retry] from the SDK default layout.
     */
    open fun getRetryButtonId(): Int = com.oxylab.sdk.startup.R.id.oxylab_splash_btn_retry

    // ── Optional Overrides for Default Layout ──

    /** Optional: Override to provide a custom App Name String resource for the default layout */
    open fun getAppNameStringResId(): Int? = null

    /** Optional: Override to provide a custom App Description/Message String resource for the default layout */
    open fun getAppDescStringResId(): Int? = null

    /** Optional: Override to provide a custom Logo Drawable resource for the default layout */
    open fun getAppLogoResId(): Int? = null

    /** Optional: Override to provide a custom Lottie Raw resource for the default layout loading animation */
    open fun getLottieAnimationResId(): Int? = null

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

    /** Optional: Whether Firebase Anonymous Auth is strictly required. Default is true. */
    open fun isFirebaseAuthRequired(): Boolean = true

    /** Optional: Theme resource for the loading ad dialog. Default is a Dialog theme. */
    open fun getLoadingDialogTheme(): Int = com.oxylab.sdk.startup.R.style.OxylabFullScreenDialogTheme

    // ── Internal State ──

    private val authState = MutableStateFlow<AuthStatus>(AuthStatus.Loading)
    private lateinit var networkMonitor: StarterNetworkMonitor
    private lateinit var nativeAdHelper: StarterNativeAdHelper
    private lateinit var bannerAdHelper: StarterBannerAdHelper
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

        // Apply custom default layout overrides if they exist and views are present
        findViewById<android.widget.ImageView>(com.oxylab.sdk.startup.R.id.oxylab_splash_app_logo)?.let { logoView ->
            getAppLogoResId()?.let { logoView.setImageResource(it) }
        }
        findViewById<TextView>(com.oxylab.sdk.startup.R.id.oxylab_splash_app_name)?.let { nameView ->
            getAppNameStringResId()?.let { nameView.setText(it) }
        }
        findViewById<TextView>(com.oxylab.sdk.startup.R.id.oxylab_splash_app_desc)?.let { descView ->
            getAppDescStringResId()?.let { descView.setText(it) }
        }
        findViewById<com.airbnb.lottie.LottieAnimationView>(com.oxylab.sdk.startup.R.id.oxylab_splash_lottie)?.let { lottieView ->
            getLottieAnimationResId()?.let { lottieView.setAnimation(it) }
        }

        // Ensure Lottie animation starts playing if the progress bar is a Lottie view
        val progressBar = findViewById<View>(getProgressBarId())
        if (progressBar is com.airbnb.lottie.LottieAnimationView) {
            progressBar.playAnimation()
        }

        findViewById<View>(getRetryButtonId())?.setOnClickListener {
            signInAnonymously()
        }

        networkMonitor = StarterNetworkMonitor(this)
        
        val configProvider = InternalAdConfig()
        val adsManager = StarterAdsManager(configProvider)
        adsManager.initialize(this)
        
        nativeAdHelper = StarterNativeAdHelper(
            this, configProvider, adsManager, networkMonitor,
            com.oxylab.sdk.startup.core.OxylabKit.nativeAdLayoutConfig
        )
        
        bannerAdHelper = StarterBannerAdHelper(configProvider, networkMonitor)
        
        interstitialAdHelper = StarterInterstitialAdHelper(
            this, configProvider, InternalAdTiming(), adsManager,
            getLoadingDialogTheme(),
            com.oxylab.sdk.startup.R.layout.dialog_loading_ad
        )

        val adContainer = findViewById<ViewGroup>(getAdContainerId())
        if (adContainer != null) {
            loadSplashAd(adContainer as? FrameLayout)
        }
        interstitialAdHelper.loadAd(getInterstitialAdUnitId(), "INTER_SPLASH")

        startAuthObservation()
        signInAnonymously()
        startReadinessLoop()
    }

    private fun signInAnonymously() {
        if (!isFirebaseAuthRequired()) {
            authState.value = AuthStatus.Authenticated
            return
        }

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
                            loadSplashAd(adContainer as? FrameLayout)
                        }
                        interstitialAdHelper.loadAd(getInterstitialAdUnitId(), "INTER_SPLASH")
                    }
                }

                val isAuthenticated = authState.value is AuthStatus.Authenticated
                if (isAuthenticated) {
                    val adContainer = findViewById<ViewGroup>(getAdContainerId())
                    val isAdReady = when (getSplashAdType()) {
                        SplashAdType.NATIVE -> adContainer != null && adContainer.childCount > 0 && adContainer.getChildAt(0) is NativeAdView
                        SplashAdType.BANNER -> adContainer != null && adContainer.childCount > 0 && adContainer.getChildAt(0) is AdView
                        SplashAdType.NONE -> true
                    }
                    val isInterLoaded = interstitialAdHelper.isAdLoaded()

                    if (isAdReady && isInterLoaded) {
                        break
                    }
                }

                delay(getPollIntervalMs())
                elapsed = System.currentTimeMillis() - startTime
            }
            proceedToApp()
        }
    }

    private fun loadSplashAd(adContainer: FrameLayout?) {
        if (adContainer == null) return
        when (getSplashAdType()) {
            SplashAdType.NATIVE -> {
                nativeAdHelper.loadNativeAdWithLayout04(getNativeAdUnitId(), adContainer, "NATIVE_SPLASH")
            }
            SplashAdType.BANNER -> {
                val bannerId = getBannerAdUnitId()
                if (bannerId.isNotEmpty()) {
                    bannerAdHelper.showBanner(this, adContainer, bannerId)
                }
            }
            SplashAdType.NONE -> {
                // Do nothing
            }
        }
    }

    private fun proceedToApp() {
        if (isProceeding) return
        isProceeding = true
        interstitialAdHelper.showAd(bypassCooldown = true) {
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
        findViewById<View>(getProgressBarId())?.visibility = View.VISIBLE
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
        override fun isAppOpenEnabled() = OxylabKit.config.isAppOpenEnabled()
        override fun isRewardedEnabled() = OxylabKit.config.isRewardedEnabled()
        override fun isAdEnabled(adVarName: String) = true
        override fun getInterstitialInterval() = OxylabKit.config.getInterstitialInterval()
    }

    private inner class InternalAdTiming : AdTimingProvider {
        override fun getLastAdTime(c: Context) = c.getSharedPreferences("oxylab_ads", 0).getLong("last_ad", 0L)
        override fun updateLastAdTime(c: Context) {
            c.getSharedPreferences("oxylab_ads", 0).edit().putLong("last_ad", System.currentTimeMillis()).apply()
        }
    }
}
