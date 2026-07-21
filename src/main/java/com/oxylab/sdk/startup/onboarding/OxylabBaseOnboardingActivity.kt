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

    // ── Required — must be provided by the app ──

    /** Ad Unit IDs */
    abstract fun getNativeAdUnitIdFullScreen(): String
    abstract fun getNativeAdUnitIdPage(position: Int): String?
    abstract fun getInterstitialAdUnitId(): String

    /** The Activity to start after onboarding finishes */
    abstract fun getNextActivityClass(): Class<out Activity>

    // ── Optional Layout Overrides (SDK ships default layouts if you skip these) ──

    /**
     * Your custom layout XML for the onboarding host screen.
     * Default: SDK built-in [R.layout.default_onboarding].
     */
    open fun getLayoutResId(): Int = com.oxylab.sdk.startup.R.layout.default_onboarding

    /**
     * The ID of your ViewPager2.
     * Default: [R.id.oxylab_onboarding_pager] from the SDK default layout.
     */
    open fun getViewPagerId(): Int = com.oxylab.sdk.startup.R.id.oxylab_onboarding_pager

    /**
     * The layout resources for your normal onboarding pages (in order).
     * Default: Generates pages based on [getOnboardingPageDataList] if provided, 
     * otherwise falls back to a single SDK built-in page [R.layout.default_onboarding_page].
     */
    open fun getPageLayouts(): List<Int> {
        val dataList = getOnboardingPageDataList()
        if (!dataList.isNullOrEmpty()) {
            return List(dataList.size) { com.oxylab.sdk.startup.R.layout.default_onboarding_page }
        }
        return listOf(com.oxylab.sdk.startup.R.layout.default_onboarding_page)
    }

    /** 
     * Optional: Override this to easily provide custom images and texts for the default onboarding layout, 
     * instead of creating your own XML layout from scratch.
     */
    open fun getOnboardingPageDataList(): List<OnboardingPageData>? {
        return listOf(
            OnboardingPageData(
                com.oxylab.sdk.startup.R.string.oxylab_default_onboarding_title_1,
                com.oxylab.sdk.startup.R.string.oxylab_default_onboarding_desc_1,
                com.oxylab.sdk.startup.R.drawable.oxylab_default_demo_image
            ),
            OnboardingPageData(
                com.oxylab.sdk.startup.R.string.oxylab_default_onboarding_title_2,
                com.oxylab.sdk.startup.R.string.oxylab_default_onboarding_desc_2,
                com.oxylab.sdk.startup.R.drawable.oxylab_default_demo_image
            ),
            OnboardingPageData(
                com.oxylab.sdk.startup.R.string.oxylab_default_onboarding_title_3,
                com.oxylab.sdk.startup.R.string.oxylab_default_onboarding_desc_3,
                com.oxylab.sdk.startup.R.drawable.oxylab_default_demo_image
            ),
            OnboardingPageData(
                com.oxylab.sdk.startup.R.string.oxylab_default_onboarding_title_4,
                com.oxylab.sdk.startup.R.string.oxylab_default_onboarding_desc_4,
                com.oxylab.sdk.startup.R.drawable.oxylab_default_demo_image
            )
        )
    }

    /** 
     * Optional: Override to perform custom view bindings on each onboarding page. 
     */
    open fun bindOnboardingPage(view: View, position: Int) {}

    /**
     * The layout resource for the page that holds the full-screen ad.
     * Default: SDK built-in [R.layout.default_onboarding_ad_page].
     */
    open fun getFullScreenAdPageLayoutResId(): Int = com.oxylab.sdk.startup.R.layout.default_onboarding_ad_page

    /**
     * The ID of the container where native ads load (inside your page layouts).
     * Default: [R.id.oxylab_ob_page_ad_container] from the SDK default page layout.
     */
    open fun getAdContainerId(): Int = com.oxylab.sdk.startup.R.id.oxylab_ob_page_ad_container

    /**
     * The ID of the container where the full-screen native ad loads.
     * Default: [R.id.oxylab_ob_ad_page_container] from the SDK default full-screen ad layout.
     */
    open fun getFullScreenAdContainerId(): Int = com.oxylab.sdk.startup.R.id.oxylab_ob_ad_page_container

    /**
     * The ID of the close button inside the full-screen ad page.
     * Default: [R.id.oxylab_ob_ad_page_close] from the SDK default ad page.
     */
    open fun getCloseButtonId(): Int = com.oxylab.sdk.startup.R.id.oxylab_ob_ad_page_close

    /**
     * The ID of the "Next" or "Start" button inside your normal pages.
     * Default: [R.id.oxylab_ob_page_btn_next] from the SDK default page layout.
     */
    open fun getNextButtonId(): Int = com.oxylab.sdk.startup.R.id.oxylab_ob_page_btn_next

    /** Optional: Theme resource for the loading ad dialog. Default is a Dialog theme. */
    open fun getLoadingDialogTheme(): Int = com.oxylab.sdk.startup.R.style.OxylabFullScreenDialogTheme

    // ── Internal State ──

    private fun getFullScreenAdPosition(): Int {
        return kotlin.math.min(2, getPageLayouts().size)
    }

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
            com.oxylab.sdk.startup.core.OxylabKit.nativeAdLayoutConfig
        )
        interstitialAdHelper = StarterInterstitialAdHelper(
            this,
            com.oxylab.sdk.startup.core.OxylabKit.config,
            com.oxylab.sdk.startup.ads.DefaultAdTimingProvider(),
            com.oxylab.sdk.startup.core.OxylabKit.adsManager,
            getLoadingDialogTheme(),
            com.oxylab.sdk.startup.R.layout.dialog_loading_ad
        )
        interstitialAdHelper.loadAd(getInterstitialAdUnitId(), "ONBOARDING_EXIT")

        val onboardingViewPager = findViewById<ViewPager2>(getViewPagerId())
        onboardingPagerAdapter = InternalOnboardingAdapter()
        onboardingViewPager?.adapter = onboardingPagerAdapter

        onboardingViewPager?.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                val isAdPage = position == getFullScreenAdPosition() && isFullScreenAdReady
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
        val child = (onboardingViewPager?.getChildAt(0) as? RecyclerView)?.layoutManager?.findViewByPosition(getFullScreenAdPosition())
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
            val adPosition = getFullScreenAdPosition()
            if (isFullScreenAdReady && position == adPosition) {
                return getFullScreenAdPageLayoutResId()
            } else {
                val originalIndex = if (isFullScreenAdReady && position > adPosition) position - 1 else position
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

                val nativeAdContainer = view.findViewById<ViewGroup>(getFullScreenAdContainerId())
                if (nativeAdContainer != null && preloadedFullScreenAd != null) {
                    if (nativeAdContainer is NativeAdView) {
                        // App provided its own full NativeAdView
                        nativeAdHelper.populateNativeAdViewFull(preloadedFullScreenAd!!, nativeAdContainer)
                        nativeAdContainer.visibility = View.VISIBLE
                        
                        // Hide shimmer if it exists (using a common ID convention)
                        val shimmerLayoutId = view.resources.getIdentifier("shimmerLayout", "id", view.context.packageName)
                        if (shimmerLayoutId != 0) {
                            view.findViewById<View>(shimmerLayoutId)?.visibility = View.GONE
                        }
                    } else if (nativeAdContainer.childCount == 0) {
                        // App provided an empty ViewGroup, inject our layout
                        val adView = LayoutInflater.from(view.context).inflate(com.oxylab.sdk.startup.R.layout.native_ad_layout_full, nativeAdContainer, false)
                        nativeAdHelper.populateNativeAdViewFull(preloadedFullScreenAd!!, adView as NativeAdView)
                        nativeAdContainer.addView(adView)
                        nativeAdContainer.visibility = View.VISIBLE
                    }
                }
            } else {
                // ── NORMAL PAGE ──
                val adPosition = getFullScreenAdPosition()
                val originalIndex = if (isFullScreenAdReady && position > adPosition) position - 1 else position
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
                val nativeAdContainer = view.findViewById<ViewGroup>(getAdContainerId())
                if (nativeAdContainer != null && !pagesWithAdsRequested.contains(position)) {
                    pagesWithAdsRequested.add(position)
                    val adId = getNativeAdUnitIdPage(position)
                    if (adId != null) {
                        nativeAdContainer.visibility = View.VISIBLE
                        nativeAdHelper.loadNativeAdWithLayout01(adId, nativeAdContainer, "ONBOARDING_PAGE_$position")
                    } else {
                        // Use INVISIBLE to maintain layout consistency and prevent shifting
                        nativeAdContainer.visibility = View.GONE
                    }
                } else if (nativeAdContainer != null) {
                    val adId = getNativeAdUnitIdPage(position)
                    if (adId == null) {
                        nativeAdContainer.visibility = View.GONE
                    } else {
                        nativeAdContainer.visibility = View.VISIBLE
                    }
                }
                
                // Bind custom text and images if using default layout overrides
                getOnboardingPageDataList()?.let { dataList ->
                    if (originalIndex < dataList.size) {
                        val data = dataList[originalIndex]
                        view.findViewById<TextView>(com.oxylab.sdk.startup.R.id.oxylab_ob_page_title)?.setText(data.titleResId)
                        view.findViewById<TextView>(com.oxylab.sdk.startup.R.id.oxylab_ob_page_desc)?.setText(data.descriptionResId)
                        view.findViewById<android.widget.ImageView>(com.oxylab.sdk.startup.R.id.oxylab_ob_page_image)?.setImageResource(data.imageResId)
                        
                        data.buttonTextResId?.let { btnTextId ->
                            view.findViewById<TextView>(getNextButtonId())?.setText(btnTextId)
                        }
                    }

                    // Dynamically rebuild dots
                    val dotContainer = view.findViewById<ViewGroup>(com.oxylab.sdk.startup.R.id.oxylab_ob_page_dots_container)
                    if (dotContainer != null) {
                        dotContainer.removeAllViews()
                        val density = view.context.resources.displayMetrics.density
                        for (i in dataList.indices) {
                            val dot = View(view.context)
                            val widthDp = if (i == originalIndex) 24 else 8
                            val marginDp = 4
                            val params = android.widget.LinearLayout.LayoutParams(
                                (widthDp * density).toInt(),
                                (8 * density).toInt()
                            )
                            val marginPx = (marginDp * density).toInt()
                            params.setMargins(marginPx, marginPx, marginPx, marginPx)
                            dot.layoutParams = params
                            dot.setBackgroundResource(
                                if (i == originalIndex) com.oxylab.sdk.startup.R.drawable.oxylab_default_dot_active
                                else com.oxylab.sdk.startup.R.drawable.oxylab_default_dot_inactive
                            )
                            dotContainer.addView(dot)
                        }
                    }
                }
                
                // Custom app binding
                bindOnboardingPage(view, originalIndex)
            }
        }

        override fun getItemCount(): Int {
            val baseCount = getPageLayouts().size
            return if (isFullScreenAdReady) baseCount + 1 else baseCount
        }
    }
}
