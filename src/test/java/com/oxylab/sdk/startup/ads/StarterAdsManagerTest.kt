package com.oxylab.sdk.startup.ads

import com.oxylab.sdk.startup.core.DefaultOxylabConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class StarterAdsManagerTest {

    @Before
    fun setUp() {
        StarterAdsManager.resetLastInterstitialTimeForTesting()
    }

    @Test
    fun defaultOxylabConfig_providesExpectedDefaults() {
        val config = DefaultOxylabConfig()
        assertTrue(config.isGlobalAdsEnabled())
        assertTrue(config.isInterstitialEnabled())
        assertTrue(config.isNativeEnabled())
        assertTrue(config.isBannerEnabled())
        assertTrue(config.isAppOpenEnabled())
        assertTrue(config.isRewardedEnabled())
        assertEquals(40_000L, config.getInterstitialInterval())
        assertFalse(config.isDebugLoggingEnabled())
    }

    @Test
    fun interstitialCooldown_elapsedWhenNoAdShown() {
        val elapsed = StarterAdsManager.isInterstitialCooldownElapsed(40_000L)
        assertTrue(elapsed)
    }

    @Test
    fun interstitialCooldown_triggersAfterAdUpdate() {
        StarterAdsManager.updateLastInterstitialTime()
        val elapsedImmediately = StarterAdsManager.isInterstitialCooldownElapsed(40_000L)
        assertFalse(elapsedImmediately)
        
        val elapsedBypassed = StarterAdsManager.isInterstitialCooldownElapsed(0L)
        assertTrue(elapsedBypassed)
    }
}
