# Consumer ProGuard rules for oxylab-startup-sdk
# These rules are applied automatically to any app that depends on this SDK.

# ── Keep all public SDK API surface ──────────────────────────────────────────
-keep public class com.oxylab.sdk.startup.splash.OxylabBaseSplashActivity { *; }
-keep public class com.oxylab.sdk.startup.language.OxylabBaseLanguageActivity { *; }
-keep public class com.oxylab.sdk.startup.language.LanguageItem { *; }
-keep public class com.oxylab.sdk.startup.onboarding.OxylabBaseOnboardingActivity { *; }
-keep public class com.oxylab.sdk.startup.core.OxylabKit { *; }
-keep public interface com.oxylab.sdk.startup.core.OxylabConfig { *; }
-keep public class com.oxylab.sdk.startup.core.DefaultOxylabConfig { *; }
-keep public class com.oxylab.sdk.startup.ads.StarterNativeAdHelper { *; }
-keep public class com.oxylab.sdk.startup.ads.StarterInterstitialAdHelper { *; }
-keep public class com.oxylab.sdk.startup.ads.StarterBannerAdHelper { *; }
-keep public class com.oxylab.sdk.startup.ads.StarterAppOpenAdHelper { *; }
-keep public class com.oxylab.sdk.startup.ads.StarterRewardedAdHelper { *; }
-keep public class com.oxylab.sdk.startup.ads.StarterAdsManager { *; }
-keep public class com.oxylab.sdk.startup.ads.NativeAdLayoutConfig { *; }
-keep public interface com.oxylab.sdk.startup.ads.AdConfigProvider { *; }
-keep public interface com.oxylab.sdk.startup.ads.AdTimingProvider { *; }
-keep public class com.oxylab.sdk.startup.ads.DefaultAdTimingProvider { *; }

# ── Keep Lottie (used by default_splash layout) ───────────────────────────────
-keep class com.airbnb.lottie.** { *; }

# ── Keep AdMob / GMS Ads ─────────────────────────────────────────────────────
-keep class com.google.android.gms.ads.** { *; }

# ── Keep Firebase Auth (used for anonymous sign-in) ──────────────────────────
-keep class com.google.firebase.auth.** { *; }

# ── Kotlin coroutines (required for SDK internals) ────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembernames class kotlinx.** {
    volatile <fields>;
}
