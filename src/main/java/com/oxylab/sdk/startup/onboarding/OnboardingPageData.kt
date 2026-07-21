package com.oxylab.sdk.startup.onboarding

/**
 * Data class to easily provide custom title, description, and image
 * for the default onboarding pages without needing custom XML layouts.
 */
data class OnboardingPageData(
    val titleResId: Int,
    val descriptionResId: Int,
    val imageResId: Int,
    val buttonTextResId: Int? = null
)
