# 🚀 Oxylab Starter Kit (SDK)

The **Oxylab Starter Kit** is a production-ready Android library designed to completely eliminate boilerplate code across multiple applications. It provides the **Splash Screen, Language Selection, Onboarding, and Ad Management** flows as simple, plug-and-play Base Activities.

Just like a premium Google SDK, you don't need to wire up complex interfaces, adapters, or engines. You simply extend our Base Activities, provide your Layout IDs, and the SDK handles the rest!

---

## ✨ Key Features

- **True Plug-and-Play**: Just extend `OxylabBaseSplashActivity` and you have a fully functional splash screen with AdMob, Firebase Auth, and Network connectivity.
- **Bundled Resources**: Native ad XML layouts, shimmer effects, and fade animations are built right in.
- **Zero Adapter Boilerplate**: The SDK automatically creates `RecyclerView` and `ViewPager2` adapters for your Language and Onboarding screens.
- **Smart Ad Management**: Built-in lazy-loading, tracking, and 3-second delay timers are handled automatically.

---

## 📦 Installation

In your new application's `settings.gradle` or `build.gradle`, include the SDK as a module:

```groovy
dependencies {
    implementation project(':startup-sdk') 
}
```

---

## 🏗️ 1. Global Initialization

Initialize the SDK exactly once in your `Application` class. This handles setting up AdMob and global ad configurations.

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        
        // Option A: Use static default values
        OxylabKit.initialize(
            context = this,
            sdkConfig = DefaultOxylabConfig(
                globalAds = true,
                interstitialAds = true,
                nativeAds = true,
                intervalMs = 40_000L // 40 seconds
            )
        )
        
        // Option B: Provide dynamic values from Firebase Remote Config!
        /*
        OxylabKit.initialize(
            context = this,
            sdkConfig = object : OxylabConfig {
                override fun isGlobalAdsEnabled() = remoteConfig.getBoolean("ads_enabled")
                override fun isInterstitialEnabled() = remoteConfig.getBoolean("interstitial_enabled")
                override fun isNativeEnabled() = remoteConfig.getBoolean("native_enabled")
                override fun isBannerEnabled() = remoteConfig.getBoolean("banner_enabled")
                override fun getInterstitialIntervalMs() = remoteConfig.getLong("interstitial_interval_ms")
            }
        )
        */
    }
}
```

---

## 💧 2. Splash Screen (Ultra Simple)

Create your `activity_splash.xml`. Then, just extend `OxylabBaseSplashActivity` and provide the view IDs. 

**The SDK will automatically handle Firebase Auth, AdMob loading loops, Network checks, and the 8-second delay timer!**

```kotlin
class SplashActivity : OxylabBaseSplashActivity() {
    
    // 1. Provide your custom UI layout
    override fun getLayoutResId() = R.layout.activity_splash
    override fun getAdContainerId() = R.id.adContainer
    
    // Provide Error/Loading View IDs
    override fun getErrorLayoutId() = R.id.layoutError
    override fun getTitleTextViewId() = R.id.tvTitle
    override fun getMessageTextViewId() = R.id.tvMessage
    override fun getProgressBarId() = R.id.progressBar
    override fun getRetryButtonId() = R.id.btnRetry

    // 2. Where to go next?
    override fun getNextActivityClass() = LanguageActivity::class.java

    // 3. Ad IDs
    override fun getNativeAdUnitId() = "ca-app-pub-XXX/YYY"
    override fun getInterstitialAdUnitId() = "ca-app-pub-XXX/ZZZ"
    
    // 4. Optional: Customize Timing & Behavior
    override fun getMinSplashTimeMs() = 8000L
    override fun getMaxWaitTimeMs() = 12000L
    override fun getOfflineMaxWaitTimeMs() = 5000L
    override fun isDarkModeEnabled() = true
}
```

---

## 🌐 3. Language Selection (Ultra Simple)

You don't need to write a `RecyclerView.Adapter`. Just provide your item layout XML and tell the SDK what languages to show!

```kotlin
class LanguageActivity : OxylabBaseLanguageActivity() {
    
    // 1. Provide your Screen Layout
    override fun getLayoutResId() = R.layout.activity_language
    override fun getRecyclerViewId() = R.id.rvLanguages
    override fun getDoneButtonId() = R.id.btnDone
    override fun getAdContainerId() = R.id.adContainer

    // 2. Provide the Data
    override fun getLanguages() = listOf(
        LanguageItem("en", "English", "English", "🇺🇸"),
        LanguageItem("hi", "Hindi", "हिंदी", "🇮🇳")
    )
    override fun getNextActivityClass() = OnboardingActivity::class.java

    // 3. Ad IDs (Dynamic based on whether user is new or returning!)
    override fun getNativeAdUnitIdInitial(isFirstTime: Boolean): String {
        return if (isFirstTime) "ca-app-pub-XXX/NEW_START" else "ca-app-pub-XXX/RETURNING_START"
    }
    
    override fun getNativeAdUnitIdSelection(isFirstTime: Boolean): String {
        return if (isFirstTime) "ca-app-pub-XXX/NEW_CLICK" else "ca-app-pub-XXX/RETURNING_CLICK"
    }

    // 4. Provide your Item Layout & Bind UI
    override fun getItemLayoutResId() = R.layout.item_language
    
    override fun bindLanguageItem(view: View, language: LanguageItem, isSelected: Boolean) {
        // The SDK passes you the inflated view!
        view.findViewById<TextView>(R.id.tvFlag).text = language.flag
        view.findViewById<TextView>(R.id.tvName).text = language.name
        
        // Example: Change border color if selected
        view.setBackgroundResource(if (isSelected) R.drawable.bg_selected else R.drawable.bg_normal)
    }
}
```
*Note: The SDK tracks whether the user is new/returning, automatically hides the "Done" button for 2 seconds, and handles the first-click ad refresh!*

---

## 📱 4. Onboarding Screen (Ultra Simple)

You don't need to write a `ViewPager2` adapter or manage dynamic full-screen ads. Just give the SDK your layout IDs!

```kotlin
class OnboardingActivity : OxylabBaseOnboardingActivity() {

    // 1. Provide Screen Layout
    override fun getLayoutResId() = R.layout.activity_onboarding
    override fun getViewPagerId() = R.id.viewPager

    // 2. Provide your normal onboarding page layouts
    override fun getPageLayouts() = listOf(
        R.layout.page_1, 
        R.layout.page_2, 
        R.layout.page_3
    )

    // Provide the layout that will wrap the Full-Screen Ad
    override fun getFullScreenAdPageLayoutResId() = R.layout.page_ad_fullscreen
    override fun getCloseButtonId() = R.id.btnCloseAd
    
    // IDs inside your pages
    override fun getAdContainerId() = R.id.adContainer
    override fun getNextButtonId() = R.id.btnNext
    
    override fun getNextActivityClass() = MainActivity::class.java

    // 3. Ad IDs
    override fun getNativeAdUnitIdFullScreen() = "ca-app-pub-XXX/FULL"
    override fun getInterstitialAdUnitId() = "ca-app-pub-XXX/EXIT"
    
    override fun getNativeAdUnitIdPage(position: Int): String? {
        return when (position) {
            0 -> "ca-app-pub-XXX/P1"
            3 -> "ca-app-pub-XXX/P3" // e.g., if you have 4 pages total
            else -> null
        }
    }
}
```
*Note: The SDK automatically injects the Full-Screen Ad at index 2 (if it loads), manages the 3-second Close button timer, and tracks lazy-loaded page ads to prevent AdMob spam.*

---

## 🛠️ Provided XML Layouts Reference
You can use these SDK-bundled layouts directly in your code via `com.oxylab.sdk.startup.R.*`:

**Layouts:**
* `R.layout.native_ad_layout_01` (to `04`)
* `R.layout.native_ad_layout_full`
* `R.layout.shimmer_native_ad_01` (to `04`)
* `R.layout.dialog_loading_ad`

**Animations:**
* `R.anim.fade_in`
* `R.anim.fade_out`

---
*Built with ❤️ by Oxylab.*
