package com.oxylab.sdk.startup.language

/**
 * Standardized data model for a language option.
 * 
 * Replaces the app-specific LanguageManager.Language data class.
 * The SDK uses this generic model to represent the language list.
 */
data class LanguageItem(
    val code: String,
    val name: String,
    val nativeName: String,
    val flag: String
)
