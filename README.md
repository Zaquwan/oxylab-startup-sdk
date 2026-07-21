# 🚀 Oxylab Starter Kit (SDK)

The **Oxylab Starter Kit** is a production-ready Android library that completely eliminates boilerplate code across multiple applications. It provides fully functional **Splash Screen, Language Selection, and Onboarding** flows as simple, plug-and-play Base Activities.

> **New in this version:** Every screen now ships with a built-in default layout. You no longer need to create XML files just to get started — only your ad unit IDs and the next Activity class are required.

---

## ✨ Key Features

| Feature | Details |
|---|---|
| **Zero-XML Quick Start** | All screens have built-in default layouts. Override only what you need. |
| **True Plug-and-Play** | Extend a Base Activity, provide ad IDs → fully working screen |
| **Bundled Ad Layouts** | Native, shimmer, and full-screen ad XML layouts built in |
| **Zero Adapter Boilerplate** | SDK creates `RecyclerView` and `ViewPager2` adapters automatically |
| **Smart Ad Management** | Lazy-loading, tracking, and 3-second delay timers handled automatically |
| **Firebase Auth** | Optional anonymous sign-in with retry / offline handling |

---

## 📦 Installation

In your application's `settings.gradle.kts`, include the SDK as a local module:

```kotlin
include(":oxylab-startup-sdk")
project(":oxylab-startup-sdk").projectDir = java.io.File("../startup-sdk-oxylab")
```

Then add the dependency in your app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation(project(":oxylab-startup-sdk"))
}
```

---

## 🏗️ 1. Global Initialization

Initialize the SDK **once** in your `Application` class:

```kotlin
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Option A: Static defaults
        OxylabKit.initialize(
            context = this,
            sdkConfig = DefaultOxylabConfig(
                globalAds = true,
                interstitialAds = true,
                nativeAds = true,
                intervalMs = 40_000L
            )
        )

        // Option B: Dynamic values from Firebase Remote Config
        /*
        OxylabKit.initialize(
            context = this,
            sdkConfig = object : OxylabConfig {
                override fun isGlobalAdsEnabled()    = remoteConfig.getBoolean("ads_enabled")
                override fun isInterstitialEnabled() = remoteConfig.getBoolean("interstitial_enabled")
                override fun isNativeEnabled()       = remoteConfig.getBoolean("native_enabled")
                override fun isBannerEnabled()       = remoteConfig.getBoolean("banner_enabled")
                override fun getInterstitialInterval() = remoteConfig.getLong("interstitial_interval_ms")
            }
        )
        */
    }
}
```

---

## 💧 2. Splash Screen

### ⚡ Minimum Setup (uses SDK default layout)

Only 3 overrides are required — the SDK provides everything else including the layout:

```kotlin
class SplashActivity : OxylabBaseSplashActivity() {
    override fun getNextActivityClass()     = LanguageActivity::class.java
    override fun getNativeAdUnitId()        = "ca-app-pub-XXX/YYY"
    override fun getInterstitialAdUnitId()  = "ca-app-pub-XXX/ZZZ"
}
```

The built-in layout includes your app name, a Lottie loading spinner, an "Ad" label, and a native ad container at the bottom.

---

### 🎨 Easy UI Customization (No XML Required)

If you want to use the default layout but change the app name and logo, simply override these methods:

```kotlin
class SplashActivity : OxylabBaseSplashActivity() {
    override fun getNextActivityClass()     = LanguageActivity::class.java
    override fun getNativeAdUnitId()        = "ca-app-pub-XXX/YYY"
    override fun getInterstitialAdUnitId()  = "ca-app-pub-XXX/ZZZ"

    // Optional: Customize default UI without creating XML
    override fun getAppNameStringResId()    = R.string.my_app_name
    override fun getAppDescStringResId()    = R.string.my_app_desc
    override fun getAppLogoResId()          = R.drawable.my_app_logo
}
```

---

### 🎨 Full Custom Layout

Override any or all of the layout methods when you want your own design:

```kotlin
class SplashActivity : OxylabBaseSplashActivity() {

    // Required
    override fun getNextActivityClass()     = LanguageActivity::class.java
    override fun getNativeAdUnitId()        = "ca-app-pub-XXX/YYY"
    override fun getInterstitialAdUnitId()  = "ca-app-pub-XXX/ZZZ"

    // Optional layout overrides
    override fun getLayoutResId()       = R.layout.activity_splash
    override fun getAdContainerId()     = R.id.adContainer
    override fun getErrorLayoutId()     = R.id.layoutError
    override fun getTitleTextViewId()   = R.id.tvTitle
    override fun getMessageTextViewId() = R.id.tvMessage
    override fun getProgressBarId()     = R.id.lottieLoader
    override fun getRetryButtonId()     = R.id.btnRetry

    // Optional behavior tweaks
    override fun getMinSplashTimeMs()       = 3000L
    override fun getMaxWaitTimeMs()         = 8000L
    override fun getOfflineMaxWaitTimeMs()  = 3000L
    override fun isDarkModeEnabled()        = true
    override fun isFirebaseAuthRequired()   = false
}
```

### Splash — Required vs Optional Reference

| Method | Required? | Default value |
|---|---|---|
| `getNextActivityClass()` | ✅ Yes | — |
| `getNativeAdUnitId()` | ✅ Yes | — |
| `getInterstitialAdUnitId()` | ✅ Yes | — |
| `getAppNameStringResId()` | ⬜ Optional | `null` |
| `getAppDescStringResId()` | ⬜ Optional | `null` |
| `getAppLogoResId()` | ⬜ Optional | `null` |
| `getLayoutResId()` | ⬜ Optional | `R.layout.default_splash` |
| `getAdContainerId()` | ⬜ Optional | `R.id.oxylab_splash_ad_container` |
| `getErrorLayoutId()` | ⬜ Optional | `R.id.oxylab_splash_layout_error` |
| `getTitleTextViewId()` | ⬜ Optional | `R.id.oxylab_splash_tv_title` |
| `getMessageTextViewId()` | ⬜ Optional | `R.id.oxylab_splash_tv_message` |
| `getProgressBarId()` | ⬜ Optional | `R.id.oxylab_splash_progress` |
| `getRetryButtonId()` | ⬜ Optional | `R.id.oxylab_splash_btn_retry` |
| `getMinSplashTimeMs()` | ⬜ Optional | `8000L` |
| `getMaxWaitTimeMs()` | ⬜ Optional | `12000L` |
| `getOfflineMaxWaitTimeMs()` | ⬜ Optional | `5000L` |
| `isDarkModeEnabled()` | ⬜ Optional | `true` |
| `isFirebaseAuthRequired()` | ⬜ Optional | `true` |

---

## 🌐 3. Language Selection

### ⚡ Minimum Setup (uses SDK default layout + default item binding)

Only 4 overrides required:

```kotlin
class LanguageActivity : OxylabBaseLanguageActivity() {
    override fun getNextActivityClass() = OnboardingActivity::class.java

    override fun getLanguages() = listOf(
        LanguageItem("en", "English", "English", "🇺🇸"),
        LanguageItem("hi", "Hindi",   "हिन्दी",  "🇮🇳"),
        LanguageItem("es", "Spanish", "Español", "🇪🇸")
    )

    override fun getNativeAdUnitIdInitial(isFirstTime: Boolean)   = "ca-app-pub-XXX/YYY"
    override fun getNativeAdUnitIdSelection(isFirstTime: Boolean) = "ca-app-pub-XXX/YYY"
}
```

The built-in item layout shows the flag emoji, language name, native name, and a checkmark for the selected language. The card background automatically switches to an accent-bordered style when selected.

---

### 🎨 Full Custom Layout

```kotlin
class LanguageActivity : OxylabBaseLanguageActivity() {

    // Required
    override fun getNextActivityClass() = OnboardingActivity::class.java

    override fun getLanguages() = listOf(
        LanguageItem("en", "English", "English", "🇺🇸"),
        LanguageItem("hi", "Hindi",   "हिन्दी",  "🇮🇳")
    )

    override fun getNativeAdUnitIdInitial(isFirstTime: Boolean): String {
        return if (isFirstTime) "ca-app-pub-XXX/NEW_START" else "ca-app-pub-XXX/RETURNING_START"
    }

    override fun getNativeAdUnitIdSelection(isFirstTime: Boolean): String {
        return if (isFirstTime) "ca-app-pub-XXX/NEW_CLICK" else "ca-app-pub-XXX/RETURNING_CLICK"
    }

    // Optional layout overrides
    override fun getLayoutResId()      = R.layout.dialog_language
    override fun getRecyclerViewId()   = R.id.recyclerView
    override fun getDoneButtonId()     = R.id.continueButton
    override fun getAdContainerId()    = R.id.languageNativeAdContiner
    override fun getItemLayoutResId()  = R.layout.item_language

    // Required only when you supply a custom item layout
    override fun bindLanguageItem(view: View, language: LanguageItem, isSelected: Boolean) {
        view.findViewById<TextView>(R.id.languageEnglishName).text = language.name
        view.findViewById<TextView>(R.id.languageNativeName).text  = language.nativeName
        val checkIcon = view.findViewById<ImageView>(R.id.checkIcon)
        checkIcon.visibility = if (isSelected) View.VISIBLE else View.GONE
    }
}
```

> **Note:** The SDK automatically hides the "Done" button for 2 seconds after the first selection, refreshes the native ad on the first language tap, and saves the selected language code to `SharedPreferences`.

### Language — Required vs Optional Reference

| Method | Required? | Default value |
|---|---|---|
| `getNextActivityClass()` | ✅ Yes | — |
| `getLanguages()` | ✅ Yes | — |
| `getNativeAdUnitIdInitial()` | ✅ Yes | — |
| `getNativeAdUnitIdSelection()` | ✅ Yes | — |
| `getLayoutResId()` | ⬜ Optional | `R.layout.default_language` |
| `getRecyclerViewId()` | ⬜ Optional | `R.id.oxylab_lang_recycler` |
| `getDoneButtonId()` | ⬜ Optional | `R.id.oxylab_lang_btn_done` |
| `getAdContainerId()` | ⬜ Optional | `R.id.oxylab_lang_ad_container` |
| `getItemLayoutResId()` | ⬜ Optional | `R.layout.default_language_item` |
| `bindLanguageItem()` | ⬜ Optional | SDK default (flag + name + check) |

---

## 📱 4. Onboarding

### ⚡ Minimum Setup (uses SDK default layout + default page)

Only 3 overrides required. The SDK inserts a single built-in onboarding page:

```kotlin
class OnboardingActivity : OxylabBaseOnboardingActivity() {
    override fun getNextActivityClass()          = MainActivity::class.java
    override fun getNativeAdUnitIdFullScreen(isFirstTime: Boolean) = "ca-app-pub-XXX/FULL"
    override fun getInterstitialAdUnitId()       = "ca-app-pub-XXX/EXIT"

    // Return null for pages that shouldn't show ads
    override fun getNativeAdUnitIdPage(position: Int, isFirstTime: Boolean): String? = null
}
```

---

### 🎨 Easy UI Customization (No XML Required)

If you want to use the default layout but need multiple pages with custom images, titles, and descriptions, return a list of `OnboardingPageData`:

```kotlin
import com.oxylab.sdk.startup.onboarding.OnboardingPageData

class OnboardingActivity : OxylabBaseOnboardingActivity() {
    override fun getNextActivityClass()          = MainActivity::class.java
    override fun getNativeAdUnitIdFullScreen(isFirstTime: Boolean) = "ca-app-pub-XXX/FULL"
    override fun getInterstitialAdUnitId()       = "ca-app-pub-XXX/EXIT"
    override fun getNativeAdUnitIdPage(position: Int, isFirstTime: Boolean) = "ca-app-pub-XXX/PAGE"

    // Optional: Customize default UI pages without creating XML
    override fun getOnboardingPageDataList() = listOf(
        OnboardingPageData(R.string.title_1, R.string.desc_1, R.drawable.image_1),
        OnboardingPageData(R.string.title_2, R.string.desc_2, R.drawable.image_2)
    )
}
```

The SDK automatically generates the correct number of pages and binds your images and texts to the default layout!

---

### 🎨 Full Custom Layout

```kotlin
class OnboardingActivity : OxylabBaseOnboardingActivity() {

    // Required
    override fun getNextActivityClass()        = MainActivity::class.java
    override fun getNativeAdUnitIdFullScreen(isFirstTime: Boolean) = "ca-app-pub-XXX/FULL"
    override fun getInterstitialAdUnitId()     = "ca-app-pub-XXX/EXIT"

    override fun getNativeAdUnitIdPage(position: Int, isFirstTime: Boolean): String? {
        return when (position) {
            0    -> "ca-app-pub-XXX/PAGE0"
            else -> null
        }
    }

    // Optional layout overrides
    override fun getLayoutResId()                = R.layout.activity_onboarding
    override fun getViewPagerId()                = R.id.viewPager
    override fun getFullScreenAdPageLayoutResId() = R.layout.item_intro_ad
    override fun getCloseButtonId()              = R.id.moveToNext
    override fun getAdContainerId()              = R.id.nativeAdContainer
    override fun getNextButtonId()               = R.id.btnNext

    override fun getPageLayouts() = listOf(
        R.layout.item_intro_0,
        R.layout.item_intro_1,
        R.layout.item_intro_2,
        R.layout.item_intro_3
    )
}
```

> **Note:** The SDK automatically inserts the full-screen ad page at index 2 (once the ad loads), manages the 3-second Close button timer, and lazy-loads per-page native ads the first time each page is displayed.

### Onboarding — Required vs Optional Reference

| Method | Required? | Default value |
|---|---|---|
| `getNextActivityClass()` | ✅ Yes | — |
| `getNativeAdUnitIdFullScreen()` | ✅ Yes | — |
| `getInterstitialAdUnitId()` | ✅ Yes | — |
| `getNativeAdUnitIdPage()` | ✅ Yes | — |
| `getOnboardingPageDataList()` | ⬜ Optional | `null` |
| `bindOnboardingPage()` | ⬜ Optional | — |
| `getLayoutResId()` | ⬜ Optional | `R.layout.default_onboarding` |
| `getViewPagerId()` | ⬜ Optional | `R.id.oxylab_onboarding_pager` |
| `getPageLayouts()` | ⬜ Optional | Auto-generated from `OnboardingPageData` if provided |
| `getFullScreenAdPageLayoutResId()` | ⬜ Optional | `R.layout.default_onboarding_ad_page` |
| `getAdContainerId()` | ⬜ Optional | `R.id.oxylab_ob_page_ad_container` |
| `getCloseButtonId()` | ⬜ Optional | `R.id.oxylab_ob_ad_page_close` |
| `getNextButtonId()` | ⬜ Optional | `R.id.oxylab_ob_page_btn_next` |

---

## 🎨 5. Global Theming & Resource Overrides

You can easily reskin the entire SDK's default layouts without writing any Kotlin or XML layouts. Simply override the SDK's built-in strings or colors by declaring resources with the exact same name in your app's `strings.xml` or `colors.xml`.

### Override Colors (in your app's `colors.xml`)

```xml
<!-- Override the SDK's default dark theme to match your branding -->
<color name="oxylab_default_bg">#FFFFFF</color>       <!-- Main background -->
<color name="oxylab_default_surface">#F5F5F5</color>  <!-- Cards/Containers -->
<color name="oxylab_default_accent">#FF0000</color>   <!-- Buttons & active states -->
<color name="oxylab_default_text_primary">#000000</color>
<color name="oxylab_default_text_secondary">#666666</color>
```

### Override Strings (in your app's `strings.xml`)

```xml
<!-- Change the global "Continue" button text across all onboarding pages -->
<string name="oxylab_default_continue">Next Step</string>

<!-- Change default loading text -->
<string name="oxylab_default_loading">Please wait...</string>
```

---

## 🎨 6. Creating a Completely Custom UI

If the default layout and resource overrides are not enough, the SDK allows you to completely replace the entire UI layout for any screen. 

The SDK separates **Business Logic** (ad loading, timers, remote config, navigation, privacy policies) from **Presentation Logic** (the actual views). You can provide your own layout XML files and just point the SDK to the views it needs to interact with.

### How it works

1. **Provide Your Layout**: Override `getLayoutResId()` and return your custom XML layout (e.g., `R.layout.my_splash_screen`).
2. **Provide the View IDs**: If the SDK needs to interact with a specific view (like a progress bar, a retry button, or a TextView for an error message), you must override the corresponding ID getter (e.g., `getProgressBarId()`) and return the ID you assigned in your layout.
3. **No Crashes on Missing Views**: The SDK uses safe `findViewById` logic. If you intentionally omit a view from your layout (like an error text view) and return an invalid ID or keep the default, the SDK simply skips interacting with it without crashing.

### Example: A Minimal Custom Splash Screen

**`res/layout/my_custom_splash.xml`**
```xml
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:gravity="center">

    <!-- Your fully custom design -->
    <ImageView android:src="@drawable/my_logo" />
    <ProgressBar android:id="@+id/my_custom_loader" />

    <FrameLayout
        android:id="@+id/my_custom_ad_container"
        android:layout_width="match_parent"
        android:layout_height="wrap_content" />
</LinearLayout>
```

**`SplashActivity.kt`**
```kotlin
class SplashActivity : OxylabBaseSplashActivity() {
    
    // Core requirements
    override fun getNextActivityClass()    = LanguageActivity::class.java
    override fun getNativeAdUnitId()       = "ca-app-pub-XXX/YYY"
    override fun getInterstitialAdUnitId() = "ca-app-pub-XXX/ZZZ"

    // 1. Tell the SDK to use your layout
    override fun getLayoutResId() = R.layout.my_custom_splash
    
    // 2. Tell the SDK where your views are
    override fun getAdContainerId() = R.id.my_custom_ad_container
    override fun getProgressBarId() = R.id.my_custom_loader
    
    // Note: We didn't provide a TitleTextView or ErrorLayout. 
    // The SDK will simply ignore those features safely!
}
```

---

## 🛠️ 7. Bundled Resource Reference

All SDK resources are accessible via `com.oxylab.sdk.startup.R.*`.

### Default Screen Layouts (new)

| Layout | Description |
|---|---|
| `R.layout.default_splash` | Full splash screen with Lottie loader + ad container |
| `R.layout.default_language` | Language screen with header, RecyclerView, and ad slot |
| `R.layout.default_language_item` | Single language row (flag · name · native name · checkmark) |
| `R.layout.default_onboarding` | Host screen with a full-screen `ViewPager2` |
| `R.layout.default_onboarding_page` | Single onboarding page (title · desc · dots · next btn · ad slot) |
| `R.layout.default_onboarding_ad_page` | Full-screen ad page with managed close button |

### Ad Layouts

| Layout | Description |
|---|---|
| `R.layout.native_ad_layout_01` – `04` | Native ad templates (various styles) |
| `R.layout.native_ad_layout_full` | Full-screen native ad layout |
| `R.layout.shimmer_native_ad_01` – `04` | Shimmer placeholders while ads load |
| `R.layout.dialog_loading_ad` | Interstitial loading dialog |

### Drawables

| Drawable | Description |
|---|---|
| `R.drawable.oxylab_default_btn_bg` | Pill-shaped button background |
| `R.drawable.oxylab_default_card_bg` | Rounded card (normal state) |
| `R.drawable.oxylab_default_card_selected_bg` | Rounded card with accent border (selected) |
| `R.drawable.oxylab_default_dot_active` | Active indicator dot |
| `R.drawable.oxylab_default_dot_inactive` | Inactive indicator dot |
| `R.drawable.oxylab_default_check_circle` | Checkmark vector icon |

### Animations

| Resource | Description |
|---|---|
| `R.anim.fade_in` | Fade-in screen transition |
| `R.anim.fade_out` | Fade-out screen transition |

---

## 📐 Custom Layout Rules

When providing your own layouts, your XML **must** contain views matching the IDs you return from the getter methods. The SDK uses `findViewById` — if a view ID is missing, the SDK safely skips it (no crash), but that feature will silently do nothing.

For the full-screen onboarding ad page, your layout must include:
- A `ViewGroup` for the ad content → ID returned by `getAdContainerId()`
- A `View` for the close button → ID returned by `getCloseButtonId()`

---

*Built with ❤️ by Oxylab.*
