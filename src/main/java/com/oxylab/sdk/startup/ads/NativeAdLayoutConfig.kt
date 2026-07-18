package com.oxylab.sdk.startup.ads

/**
 * Configuration data class holding all layout and view resource IDs
 * needed by [StarterNativeAdHelper].
 *
 * The host application provides these from its own R class:
 *
 * ```kotlin
 * val layoutConfig = NativeAdLayoutConfig(
 *     nativeLayout01 = R.layout.native_ad_layout_01,
 *     nativeLayout02 = R.layout.native_ad_layout_02,
 *     nativeLayout03 = R.layout.native_ad_layout_03,
 *     nativeLayout04 = R.layout.native_ad_layout_04,
 *     nativeLayoutFull = R.layout.native_ad_layout_full,
 *     shimmer01 = R.layout.shimmer_native_ad_01,
 *     shimmer02 = R.layout.shimmer_native_ad_02,
 *     shimmer03 = R.layout.shimmer_native_ad_03,
 *     shimmer04 = R.layout.shimmer_native_ad_04,
 *     shimmerContainerId = R.id.shimmer_view_container,
 *     headlineId = R.id.native_ad_headline,
 *     mediaViewId = R.id.native_ad_media_view,
 *     bodyId = R.id.native_ad_body,
 *     callToActionId = R.id.native_ad_call_to_action,
 *     iconId = R.id.native_ad_icon,
 *     adChoicesId = R.id.native_ad_choice_view,
 *     advertiserId = R.id.native_ad_advertiser,
 *     storeId = R.id.native_ad_store,
 *     priceId = R.id.native_ad_price,
 * )
 * ```
 */
data class NativeAdLayoutConfig(
    // Native ad layout resource IDs
    val nativeLayout01: Int,
    val nativeLayout02: Int,
    val nativeLayout03: Int,
    val nativeLayout04: Int,
    val nativeLayoutFull: Int,

    // Shimmer layout resource IDs
    val shimmer01: Int,
    val shimmer02: Int,
    val shimmer03: Int,
    val shimmer04: Int,

    // Shimmer container view ID
    val shimmerContainerId: Int,

    // Standard native ad view IDs (used inside all layout variants)
    val headlineId: Int,
    val mediaViewId: Int,
    val bodyId: Int,
    val callToActionId: Int,
    val iconId: Int,
    val adChoicesId: Int,

    // Extended view IDs (used only in Full layout)
    val advertiserId: Int = 0,
    val storeId: Int = 0,
    val priceId: Int = 0,
)
