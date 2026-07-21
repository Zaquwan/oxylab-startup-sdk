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

    // ── Required — must be provided by the app ──

    /** The Activity to start after the user clicks Done */
    abstract fun getNextActivityClass(): Class<out Activity>

    /** Provide the list of languages you want to show */
    abstract fun getLanguages(): List<LanguageItem>

    /** Ad Unit ID for the first time the screen opens. */
    abstract fun getNativeAdUnitIdInitial(isFirstTime: Boolean): String

    /** Ad Unit ID for when the user taps a language (the refresh ad). */
    abstract fun getNativeAdUnitIdSelection(isFirstTime: Boolean): String

    // ── Optional Layout Overrides (SDK ships default layouts if you skip these) ──

    /**
     * Your custom layout XML for the language screen.
     * Default: SDK built-in [R.layout.default_language].
     */
    open fun getLayoutResId(): Int = com.oxylab.sdk.startup.R.layout.default_language

    /**
     * The ID of your RecyclerView.
     * Default: [R.id.oxylab_lang_recycler] from the SDK default layout.
     */
    open fun getRecyclerViewId(): Int = com.oxylab.sdk.startup.R.id.oxylab_lang_recycler

    /**
     * The ID of the Done/Next button.
     * Default: [R.id.oxylab_lang_btn_done] from the SDK default layout.
     */
    open fun getDoneButtonId(): Int = com.oxylab.sdk.startup.R.id.oxylab_lang_btn_done

    /**
     * The ID of the FrameLayout where native ads will load.
     * Default: [R.id.oxylab_lang_ad_container] from the SDK default layout.
     */
    open fun getAdContainerId(): Int = com.oxylab.sdk.startup.R.id.oxylab_lang_ad_container

    /**
     * The layout for a single language item in the list.
     * Default: SDK built-in [R.layout.default_language_item].
     */
    open fun getItemLayoutResId(): Int = com.oxylab.sdk.startup.R.layout.default_language_item

    /**
     * Bind language data to a list item view.
     * Default implementation reads IDs from [R.layout.default_language_item].
     * Override when you supply your own [getItemLayoutResId].
     */
    open fun bindLanguageItem(view: View, language: LanguageItem, isSelected: Boolean) {
        view.findViewById<android.widget.TextView>(com.oxylab.sdk.startup.R.id.oxylab_lang_item_flag)
            ?.text = language.flag
        view.findViewById<android.widget.TextView>(com.oxylab.sdk.startup.R.id.oxylab_lang_item_name)
            ?.text = language.name
        view.findViewById<android.widget.TextView>(com.oxylab.sdk.startup.R.id.oxylab_lang_item_native)
            ?.text = language.nativeName
        val check = view.findViewById<android.widget.ImageView>(com.oxylab.sdk.startup.R.id.oxylab_lang_item_check)
        check?.visibility = if (isSelected) View.VISIBLE else View.GONE
        view.background = if (isSelected)
            view.context.getDrawable(com.oxylab.sdk.startup.R.drawable.oxylab_default_card_selected_bg)
        else
            view.context.getDrawable(com.oxylab.sdk.startup.R.drawable.oxylab_default_card_bg)
    }

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
            com.oxylab.sdk.startup.core.OxylabKit.nativeAdLayoutConfig
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
                    val lang = items[bindingAdapterPosition]
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
