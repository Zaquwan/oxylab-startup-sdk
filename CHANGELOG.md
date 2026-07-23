# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.7] - 2026-07-23
### Added
- **Firebase Remote Config Engine**: Built-in `FirebaseOxylabConfig` implementing single-fetch in-memory session caching.
- **Remote Config String Convention**: Supports `"1"` (enabled) and `"0"` (disabled) string values in Firebase Console.
- **Offline Defaults & Throttling Support**: Added `setDefaultsAsync()` and `setFetchInterval()` helpers to `FirebaseOxylabConfig`.
- **Per-Placement `adVarName` Controls**: Updated `StarterBannerAdHelper`, `StarterAppOpenAdHelper`, `StarterRewardedAdHelper`, `StarterInterstitialAdHelper`, and `StarterNativeAdHelper` to check `isAdEnabled(adVarName)`.
- **Customizable Base Activity Key Overrides**: Added overridable `adVarName` getters for `OxylabBaseSplashActivity`, `OxylabBaseLanguageActivity`, and `OxylabBaseOnboardingActivity`.
- **Automatic `onDestroy()` Lifecycle Cleanup**: Automatically releases native ads, banner views, and cancels coroutine timer jobs in all Base Activities to ensure zero memory leaks.
- **Global Interstitial Cooldown**: Centralized thread-safe interstitial cooldown with per-placement bypass support (`isInterstitialCooldownBypassed`).

## [1.2.6] - 2024-07-23
### Added
- Feature: Added Banner Ad support for the splash screen (`SplashAdType.BANNER`).
- Updated README.md with instructions for Splash Screen banner ads.

## [1.2.5]
### Added
- Feature: `excludeActivity` functionality added to `StarterAppOpenAdHelper` to prevent App Open Ads on specific screens.
- Updated README.md with `excludeActivity` usage.

## [1.2.4]
### Fixed
- Bugfix: Corrected the onboarding page ad index loading position.

## [1.2.3]
### Changed
- Updated `OnboardingActivity` abstract methods to include the `isFirstTime` boolean for native ad IDs.
- Updated README with a "Completely Custom UI" section.

## [1.2.2]
### Fixed
- Fixed `targetCompatibility` and `sourceCompatibility` to match Kotlin `jvmToolchain`.

## [1.2.1]
### Fixed
- Fixed standalone JitPack build by adding `gradle-wrapper.jar`, `settings.gradle.kts`, and correct plugin versions.

## [1.2.0]
### Added
- Updated README with global theming & resource override documentation.

*(Note: Add previous versions manually as needed)*
