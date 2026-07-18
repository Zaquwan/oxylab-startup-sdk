package com.oxylab.sdk.startup.onboarding

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.style.ClickableSpan
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.oxylab.sdk.startup.ads.StarterInterstitialAdHelper
import com.oxylab.sdk.startup.ads.StarterNativeAdHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Plug-and-play Base Activity for the Onboarding Screen.
 * 
 * Hides all ViewPager adapter complexity, dynamic full-screen ad insertion,
 * 3-second close button timers, and lazy-loading tracking.
 */
abstract class OxylabBaseOnboardingActivity : AppCompatActivity() {

    // ── Developer Must Provide These ──

    /** Your custom layout XML for the onboarding screen (e.g. R.layout.activity_onboarding) */
    abstract fun getLayoutResId(): Int
    
    /** The ID of your ViewPager2 */
    abstract fun getViewPagerId(): Int
    
    /** 
     * The layout resources for your normal onboarding pages (in order). 
     * e.g., listOf(R.layout.page_1, R.layout.page_2, R.layout.page_3)
     */
    abstract fun getPageLayouts(): List<Int>
    
    /** The layout resource for the page that will hold the full-screen ad. 
     * It MUST contain a FrameLayout/ViewGroup for the ad, and a Close button. */
    abstract fun getFullScreenAdPageLayoutResId(): Int

    /** The ID of the container where native ads should load (inside your page layouts) */
    abstract fun getAdContainerId(): Int
    
    /** The ID of the close button inside your full-screen ad page layout */
    abstract fun getCloseButtonId(): Int
    
    /** The ID of the "Next" or "Start" button inside your normal pages */
    abstract fun getNextButtonId(): Int

    /** Ad Unit IDs */
    abstract fun getNativeAdUnitIdFullScreen(): String
    abstract fun getNativeAdUnitIdPage(position: Int): String?
    abstract fun getInterstitialAdUnitId(): String

    /** The Activity to start after onboarding finishes */
    abstract fun getNextActivityClass(): Class<out Activity>

    // ── Internal State ──

    private var isFullScreenAdReady = false
    private var shouldShowCloseButton = false
    private var closeButtonDelayJob: Job? = null
    private val pagesWithAdsRequested = mutableSetOf<Int>()

    private lateinit var onboardingPagerAdapter: InternalOnboardingAdapter
    private lateinit var nativeAdHelper: StarterNativeAdHelper
    private lateinit var interstitialAdHelper: StarterInterstitialAdHelper
    private var preloadedFullScreenAd: NativeAd? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(getLayoutResId())

        nativeAdHelper = StarterNativeAdHelper(
            this,
            com.oxylab.sdk.startup.core.OxylabKit.config,
            com.oxylab.sdk.startup.core.OxylabKit.adsManager,
            com.oxylab.sdk.startup.utils.StarterNetworkMonitor(this),
            com.oxylab.sdk.startup.ads.NativeAdLayoutConfig()
        )
        interstitialAdHelper = StarterInterstitialAdHelper(
            this,
            com.oxylab.sdk.startup.core.OxylabKit.config,
            com.oxylab.sdk.startup.ads.DefaultAdTimingProvider(),
            com.oxylab.sdk.startup.core.OxylabKit.adsManager,
            android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar,
            com.oxylab.sdk.startup.R.layout.dialog_loading_ad
        )
        interstitialAdHelper.loadAd(getInterstitialAdUnitId(), "ONBOARDING_EXIT")

        val onboardingViewPager = findViewById<ViewPager2>(getViewPagerId())
        onboardingPagerAdapter = InternalOnboardingAdapter()
        onboardingViewPager?.adapter = onboardingPagerAdapter

        onboardingViewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val isAdPage = position == 2 && isFullScreenAdReady
                handlePageSelected(isAdPage)
            }
        })

        // Preload full screen ad
        val prefs = getSharedPreferences("oxylab_onboarding_ads", Context.MODE_PRIVATE)
        val isFirstTimeFullScreenAdLoaded = prefs.getBoolean("is_first_full", true)
        
        nativeAdHelper.loadNativeAd(getNativeAdUnitIdFullScreen(), "ONBOARDING_FULL", onLoaded = { loadedAd ->
            preloadedFullScreenAd = loadedAd
            isFullScreenAdReady = true
            if (isFirstTimeFullScreenAdLoaded) {
                prefs.edit().putBoolean("is_first_full", false).apply()
            }
            // Notify adapter that ad is ready (changes page count and layout)
            onboardingPagerAdapter.notifyDataSetChanged()
        }, onFailed = {})
    }

    private fun handlePageSelected(isAdPage: Boolean) {
        closeButtonDelayJob?.cancel()
        
        if (isAdPage) {
            shouldShowCloseButton = false
            updateCloseButtonVisibility()
            
            closeButtonDelayJob = lifecycleScope.launch {
                delay(3000L)
                shouldShowCloseButton = true
                updateCloseButtonVisibility()
            }
        } else {
            shouldShowCloseButton = false
        }
    }

    private fun updateCloseButtonVisibility() {
        // Need to find the close button in the active view pager child.
        val onboardingViewPager = findViewById<ViewPager2>(getViewPagerId())
        val child = (onboardingViewPager?.getChildAt(0) as? RecyclerView)?.layoutManager?.findViewByPosition(2)
        val closeAdButton = child?.findViewById<View>(getCloseButtonId())
        closeAdButton?.visibility = if (shouldShowCloseButton) View.VISIBLE else View.GONE
    }

    /** Call this method from your final page's "Start" button click listener to finish onboarding */
    fun finishOnboarding() {
        interstitialAdHelper.showAd {
            val intent = Intent(this, getNextActivityClass())
            startActivity(intent)
            @Suppress("DEPRECATION")
            overridePendingTransition(com.oxylab.sdk.startup.R.anim.fade_in, com.oxylab.sdk.startup.R.anim.fade_out)
            finish()
        }
    }

    /**
     * Utility to make a portion of a TextView clickable and open a URL.
     * 
     * @param textView The TextView to modify.
     * @param fullText The complete text string.
     * @param clickablePart The exact substring that should be clickable.
     * @param url The URL to open when clicked.
     */
    fun setupClickableSpan(textView: TextView, fullText: String, clickablePart: String, url: String) {
        val spannableString = SpannableString(fullText)
        val clickableSpan = object : ClickableSpan() {
            override fun onClick(widget: View) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                textView.context.startActivity(intent)
            }
        }

        val startIndex = fullText.indexOf(clickablePart)
        if (startIndex != -1) {
            val endIndex = startIndex + clickablePart.length
            spannableString.setSpan(
                clickableSpan,
                startIndex,
                endIndex,
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            textView.text = spannableString
            textView.movementMethod = android.text.method.LinkMovementMethod.getInstance()
        } else {
            textView.text = fullText
        }
    }

    // ── Internal Helpers ──

    private inner class PageViewHolder(view: View) : RecyclerView.ViewHolder(view)

    private inner class InternalOnboardingAdapter : RecyclerView.Adapter<PageViewHolder>() {
        
        override fun getItemViewType(position: Int): Int {
            if (isFullScreenAdReady && position == 2) {
                return getFullScreenAdPageLayoutResId()
            } else {
                val originalIndex = if (isFullScreenAdReady && position > 2) position - 1 else position
                val layouts = getPageLayouts()
                return layouts[originalIndex]
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(viewType, parent, false)
            return PageViewHolder(view)
        }

        override fun onBindViewHolder(holder: PageViewHolder, position: Int) {
            val view = holder.itemView
            val viewType = getItemViewType(position)
            val viewPager = findViewById<ViewPager2>(getViewPagerId())

            if (viewType == getFullScreenAdPageLayoutResId()) {
                // ── FULL SCREEN AD PAGE ──
                val closeAdButton = view.findViewById<View>(getCloseButtonId())
                closeAdButton?.visibility = if (shouldShowCloseButton) View.VISIBLE else View.GONE
                
                closeAdButton?.setOnClickListener { 
                    viewPager?.currentItem = (viewPager?.currentItem ?: 0) + 1 
                }

                val nativeAdContainer = view.findViewById<ViewGroup>(getAdContainerId())
                if (nativeAdContainer != null && nativeAdContainer.childCount == 0 && preloadedFullScreenAd != null) {
                    val adView = LayoutInflater.from(view.context).inflate(com.oxylab.sdk.startup.R.layout.native_ad_layout_full, nativeAdContainer, false)
                    nativeAdHelper.populateNativeAdViewFull(preloadedFullScreenAd!!, adView as NativeAdView)
                    nativeAdContainer.addView(adView)
                }
            } else {
                // ── NORMAL PAGE ──
                val originalIndex = if (isFullScreenAdReady && position > 2) position - 1 else position
                val layouts = getPageLayouts()
                
                val nextPageButton = view.findViewById<View>(getNextButtonId())
                nextPageButton?.setOnClickListener {
                    if (originalIndex == layouts.size - 1) {
                        finishOnboarding()
                    } else {
                        viewPager?.currentItem = (viewPager?.currentItem ?: 0) + 1 
                    }
                }

                // Request Lazy Ad Load
                if (viewPager?.currentItem == position) {
                    val nativeAdContainer = view.findViewById<ViewGroup>(getAdContainerId())
                    if (nativeAdContainer != null && !pagesWithAdsRequested.contains(position)) {
                        pagesWithAdsRequested.add(position)
                        val adId = getNativeAdUnitIdPage(position)
                        if (adId != null) {
                            nativeAdHelper.loadNativeAdWithLayout01(adId, nativeAdContainer, "ONBOARDING_PAGE_$position")
                        }
                    }
                }
            }
        }

        override fun getItemCount(): Int {
            val baseCount = getPageLayouts().size
            return if (isFullScreenAdReady) baseCount + 1 else baseCount
        }
    }
}
