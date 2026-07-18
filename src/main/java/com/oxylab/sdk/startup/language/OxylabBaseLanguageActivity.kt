package com.oxylab.sdk.startup.language

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.oxylab.sdk.startup.ads.StarterNativeAdHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Plug-and-play Base Activity for the Language Selection Screen.
 * 
 * Hides all ad refresh logic, the 2-second 'Done' button delay, and SharedPreferences tracking.
 */
abstract class OxylabBaseLanguageActivity : AppCompatActivity() {

    // ── Developer Must Provide These ──

    /** Your custom layout XML for the language screen (e.g. R.layout.activity_language) */
    abstract fun getLayoutResId(): Int
    
    /** The ID of your RecyclerView */
    abstract fun getRecyclerViewId(): Int
    
    /** The ID of the Done/Next button (will be hidden automatically for 2 seconds) */
    abstract fun getDoneButtonId(): Int
    
    /** The ID of the FrameLayout where native ads will load */
    abstract fun getAdContainerId(): Int

    /** The layout for a single language item in the list (e.g. R.layout.item_language) */
    abstract fun getItemLayoutResId(): Int

    /** The Activity to start after the user clicks Done */
    abstract fun getNextActivityClass(): Class<out Activity>
    
    /** Provide the list of languages you want to show */
    abstract fun getLanguages(): List<LanguageItem>
    
    /** Ad Unit ID for the first time the screen opens. 
     * @param isFirstTime True if this is the very first time the user has opened this screen. */
    abstract fun getNativeAdUnitIdInitial(isFirstTime: Boolean): String
    
    /** Ad Unit ID for when the user taps a language (the refresh ad). 
     * @param isFirstTime True if this is the very first time the user is selecting a language. */
    abstract fun getNativeAdUnitIdSelection(isFirstTime: Boolean): String

    /** 
     * Bind the data to your custom layout. 
     * @param view The root view of your inflated item layout.
     * @param language The language object.
     * @param isSelected True if this is the currently selected language.
     */
    abstract fun bindLanguageItem(view: View, language: LanguageItem, isSelected: Boolean)

    // ── Internal State ──

    private var selectedLanguageCode: String? = null
    private var isDoneButtonVisible = false
    private var doneTimerJob: Job? = null
    private var isAdRefreshed = false

    private lateinit var nativeAdHelper: StarterNativeAdHelper
    private lateinit var languageAdapter: InternalLanguageAdapter

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
        
        // Load initial selected code from preferences
        val prefs = getSharedPreferences("oxylab_lang", Context.MODE_PRIVATE)
        selectedLanguageCode = prefs.getString("selected_lang", "en")

        val recyclerView = findViewById<RecyclerView>(getRecyclerViewId())
        val btnDone = findViewById<View>(getDoneButtonId())
        val adContainer = findViewById<ViewGroup>(getAdContainerId())

        languageAdapter = InternalLanguageAdapter()
        recyclerView?.layoutManager = LinearLayoutManager(this)
        recyclerView?.adapter = languageAdapter

        btnDone?.visibility = View.GONE

        btnDone?.setOnClickListener { 
            val fromSettings = intent.getBooleanExtra("from_settings", false)
            selectedLanguageCode?.let { code ->
                prefs.edit().putString("selected_lang", code).apply()
                val nextIntent = Intent(this, getNextActivityClass())
                if (fromSettings) {
                    nextIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(nextIntent)
                finish()
            }
        }

        if (adContainer != null) {
            val adPrefs = getSharedPreferences("oxylab_lang_ad", Context.MODE_PRIVATE)
            val isFirst = adPrefs.getBoolean("is_first_ad", true)
            nativeAdHelper.loadNativeAdWithLayout01(getNativeAdUnitIdInitial(isFirst), adContainer, "LANG_INITIAL") {
                if (isFirst) adPrefs.edit().putBoolean("is_first_ad", false).apply()
            }
        }
    }

    private fun handleLanguageSelection(code: String) {
        selectedLanguageCode = code
        languageAdapter.notifyDataSetChanged()

        // Handle the Done button visibility delay
        val btnDone = findViewById<View>(getDoneButtonId())
        
        if (!isDoneButtonVisible) {
            isDoneButtonVisible = false
            btnDone?.visibility = View.GONE
            doneTimerJob?.cancel()
            doneTimerJob = lifecycleScope.launch {
                delay(2000L)
                isDoneButtonVisible = true
                btnDone?.visibility = View.VISIBLE
            }
        }

        // Refresh ad on FIRST selection only
        if (!isAdRefreshed) {
            isAdRefreshed = true
            val adContainer = findViewById<ViewGroup>(getAdContainerId())
            if (adContainer != null) {
                val adPrefs = getSharedPreferences("oxylab_lang_ad", Context.MODE_PRIVATE)
                val isFirstSelect = adPrefs.getBoolean("is_first_select", true)
                nativeAdHelper.loadNativeAdWithLayout01(getNativeAdUnitIdSelection(isFirstSelect), adContainer, "LANG_SELECT") {
                    if (isFirstSelect) adPrefs.edit().putBoolean("is_first_select", false).apply()
                }
            }
        }
    }

    // ── Internal Helpers ──

    private inner class InternalLanguageAdapter : RecyclerView.Adapter<InternalLanguageAdapter.LanguageViewHolder>() {
        
        private val items = getLanguages()

        inner class LanguageViewHolder(val view: View) : RecyclerView.ViewHolder(view) {
            init {
                view.setOnClickListener {
                    val lang = items[adapterPosition]
                    handleLanguageSelection(lang.code)
                }
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(getItemLayoutResId(), parent, false)
            return LanguageViewHolder(view)
        }

        override fun onBindViewHolder(holder: LanguageViewHolder, position: Int) {
            val lang = items[position]
            bindLanguageItem(holder.view, lang, lang.code == selectedLanguageCode)
        }

        override fun getItemCount() = items.size
    }
}
