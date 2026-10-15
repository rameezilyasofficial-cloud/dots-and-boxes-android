package com.fixtocode.dotsandboxes

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.CountDownTimer
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import kotlin.concurrent.thread

data class SponsoredAdItem(
    val id: Int,
    val enabled: Boolean,
    val mediaType: String,
    val mediaUrl: String,
    val badge: String,
    val headline: String,
    val description: String,
    val buttonText: String,
    val targetUrl: String
)

class MainActivity : AppCompatActivity() {

    companion object {
        const val APP_RELEASE_NUMBER = 1
        const val OFFICIAL_WEB_URL = "https://dotsandboxe.netlify.app"
        const val DIRECT_APK_URL = "https://dotsandboxe.netlify.app/dotandboxes.apk"
        const val LIVE_CLOUD_DB_URL = "https://dotsandboxe.netlify.app/version.json"
        const val UNITY_GAME_ID = "800398416"
        const val UNITY_TEST_MODE = false

        const val TWELVE_HOURS_MS = 12L * 60L * 60L * 1000L
        const val TWENTY_FOUR_HOURS_MS = 24L * 60L * 60L * 1000L
    }

    private lateinit var prefs: SharedPreferences

    private lateinit var configContainer: View
    private lateinit var arenaContainer: View
    private lateinit var levelOptionsLayout: LinearLayout
    private lateinit var tvLevelInfo: TextView
    private lateinit var tvTotalBank: TextView
    private lateinit var gridOptionsLayout: LinearLayout
    private lateinit var playerCountTabsLayout: LinearLayout
    private lateinit var rosterLayout: LinearLayout
    private lateinit var tvTurnIndicator: TextView
    private lateinit var arenaScoresLayout: LinearLayout
    private lateinit var gameView: GameView

    // Settings & Update Button
    private lateinit var switchSound: Switch
    private lateinit var switchVibration: Switch
    private lateinit var switchSpeedAnalytics: Switch
    private lateinit var btnCheckUpdateManual: Button

    // Victory Screen Overlay Views
    private lateinit var winnerOverlay: FrameLayout
    private lateinit var tvWinnerTitle: TextView
    private lateinit var tvWinnerSubtitle: TextView
    private lateinit var winnerStatsLayout: LinearLayout
    private lateinit var tvAnalyticsHeading: TextView
    private lateinit var winnerAnalyticsLayout: LinearLayout
    private lateinit var btnOverlayPlayAgain: Button
    private lateinit var btnOverlaySettings: Button

    // 4-Slot Sequential Picture/Video Ad Overlay Views
    private lateinit var adOverlay: FrameLayout
    private lateinit var tvAdBadge: TextView
    private lateinit var tvAdCountdown: TextView
    private lateinit var ivAdPicture: ImageView
    private lateinit var vvAdVideo: VideoView
    private lateinit var tvAdFallbackIcon: TextView
    private lateinit var pbAdMediaLoading: ProgressBar
    private lateinit var tvAdHeadline: TextView
    private lateinit var tvAdDescription: TextView
    private lateinit var btnAdAction: Button
    private var adTimer: CountDownTimer? = null
    private var isAdSkippable: Boolean = false
    private var currentAdTargetUrl: String = OFFICIAL_WEB_URL
    private var currentAdMode: String = "hybrid"
    private val bitmapCache = mutableMapOf<String, Bitmap>()

    // In-App Update Overlay Views
    private lateinit var updateOverlay: FrameLayout
    private lateinit var tvUpdateTitle: TextView
    private lateinit var tvUpdateMessage: TextView
    private lateinit var btnUpdateNow: Button
    private lateinit var btnUpdateLater: Button

    private val playerColors = intArrayOf(
        0xFFEF4444.toInt(), // P1 Red
        0xFFF59E0B.toInt(), // P2 Amber
        0xFF06B6D4.toInt(), // P3 Cyan
        0xFF10B981.toInt(), // P4 Green
        0xFF8B5CF6.toInt()  // P5 Violet
    )

    private var selectedLevel: Int = 1
    private var selectedRows = 5
    private var selectedCols = 5
    private var playerCount = 2
    private val activePlayers = mutableListOf<Player>()
    private var gridPresets: List<Pair<String, Pair<Int, Int>>> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences("dots_boxes_progress", Context.MODE_PRIVATE)
        selectedLevel = getUnlockedLevel()

        // Restore saved user preferences (Default 5x5 on first install, then remembers choice)
        selectedRows = prefs.getInt("user_pref_rows", 5)
        selectedCols = prefs.getInt("user_pref_cols", 5)
        playerCount = prefs.getInt("user_pref_player_count", 2).coerceIn(2, 5)

        configContainer = findViewById(R.id.configContainer)
        arenaContainer = findViewById(R.id.arenaContainer)
        levelOptionsLayout = findViewById(R.id.levelOptionsLayout)
        tvLevelInfo = findViewById(R.id.tvLevelInfo)
        tvTotalBank = findViewById(R.id.tvTotalBank)
        gridOptionsLayout = findViewById(R.id.gridOptionsLayout)
        playerCountTabsLayout = findViewById(R.id.playerCountTabsLayout)
        rosterLayout = findViewById(R.id.rosterLayout)
        tvTurnIndicator = findViewById(R.id.tvTurnIndicator)
        arenaScoresLayout = findViewById(R.id.arenaScoresLayout)
        gameView = findViewById(R.id.gameView)

        switchSound = findViewById(R.id.switchSound)
        switchVibration = findViewById(R.id.switchVibration)
        switchSpeedAnalytics = findViewById(R.id.switchSpeedAnalytics)
        btnCheckUpdateManual = findViewById(R.id.btnCheckUpdateManual)

        winnerOverlay = findViewById(R.id.winnerOverlay)
        tvWinnerTitle = findViewById(R.id.tvWinnerTitle)
        tvWinnerSubtitle = findViewById(R.id.tvWinnerSubtitle)
        winnerStatsLayout = findViewById(R.id.winnerStatsLayout)
        tvAnalyticsHeading = findViewById(R.id.tvAnalyticsHeading)
        winnerAnalyticsLayout = findViewById(R.id.winnerAnalyticsLayout)
        btnOverlayPlayAgain = findViewById(R.id.btnOverlayPlayAgain)
        btnOverlaySettings = findViewById(R.id.btnOverlaySettings)

        adOverlay = findViewById(R.id.adOverlay)
        tvAdBadge = findViewById(R.id.tvAdBadge)
        tvAdCountdown = findViewById(R.id.tvAdCountdown)
        ivAdPicture = findViewById(R.id.ivAdPicture)
        vvAdVideo = findViewById(R.id.vvAdVideo)
        tvAdFallbackIcon = findViewById(R.id.tvAdFallbackIcon)
        pbAdMediaLoading = findViewById(R.id.pbAdMediaLoading)
        tvAdHeadline = findViewById(R.id.tvAdHeadline)
        tvAdDescription = findViewById(R.id.tvAdDescription)
        btnAdAction = findViewById(R.id.btnAdAction)

        updateOverlay = findViewById(R.id.updateOverlay)
        tvUpdateTitle = findViewById(R.id.tvUpdateTitle)
        tvUpdateMessage = findViewById(R.id.tvUpdateMessage)
        btnUpdateNow = findViewById(R.id.btnUpdateNow)
        btnUpdateLater = findViewById(R.id.btnUpdateLater)

        setupAdaptiveGridOptions()
        setupPlayerCountTabs()
        setupLevelSelectorBar()
        updateLevelBannerText(selectedLevel)
        renderRoster()

        // Initialize Live Unity Ads SDK (800398416)
        UnityAdsBridge.initialize(this, UNITY_GAME_ID, UNITY_TEST_MODE)

        // Sync 4-Ad Cloud Studio & Check for App Updates
        syncFourAdsAndCheckUpdate(forceCheckOnStartup = true, manualUserCheck = false)

        btnCheckUpdateManual.setOnClickListener {
            Toast.makeText(this, "Checking for latest update...", Toast.LENGTH_SHORT).show()
            syncFourAdsAndCheckUpdate(forceCheckOnStartup = true, manualUserCheck = true)
        }

        findViewById<Button>(R.id.btnStartMatch).setOnClickListener { startMatch() }
        findViewById<ImageButton>(R.id.btnLeaveArena).setOnClickListener { returnToConfig() }
        findViewById<ImageButton>(R.id.btnResetMatch).setOnClickListener { startMatch() }

        btnOverlayPlayAgain.setOnClickListener {
            winnerOverlay.visibility = View.GONE
            startMatch()
        }
        btnOverlaySettings.setOnClickListener {
            winnerOverlay.visibility = View.GONE
            returnToConfig()
        }

        btnAdAction.setOnClickListener {
            openWebUrl(currentAdTargetUrl)
        }

        tvAdCountdown.setOnClickListener {
            if (isAdSkippable) {
                stopAdMediaPlayback()
                adOverlay.visibility = View.GONE
                showVictoryModal()
            }
        }

        btnUpdateLater.setOnClickListener {
            updateOverlay.visibility = View.GONE
        }

        gameView.onTurnCompleted = {
            updateArenaHud()
            if (gameView.engine?.isGameOver == true) {
                gameView.postDelayed({ handleMatchEndWithAdCheck() }, 350L)
            }
        }
    }

    /**
     * Fetches the 4-Ad Cloud Database + Checks for New App Updates.
     */
    private fun syncFourAdsAndCheckUpdate(forceCheckOnStartup: Boolean, manualUserCheck: Boolean) {
        val now = System.currentTimeMillis()
        val lastAdSyncMs = prefs.getLong("last_sponsored_ad_sync_ms", 0L)
        val lastUpdateCheckMs = prefs.getLong("last_version_check_ms", 0L)

        val needAdRefresh = forceCheckOnStartup || ((now - lastAdSyncMs) >= TWELVE_HOURS_MS)
        val needVersionCheck = manualUserCheck || ((now - lastUpdateCheckMs) >= TWENTY_FOUR_HOURS_MS)

        if (!needAdRefresh && !needVersionCheck) return

        thread(start = true) {
            var json: JSONObject? = null

            val urlsToTry = listOf(
                LIVE_CLOUD_DB_URL,
                "$OFFICIAL_WEB_URL/version.json?t=$now"
            )

            for (endpoint in urlsToTry) {
                try {
                    val conn = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                        requestMethod = "GET"
                        connectTimeout = 5000
                        readTimeout = 5000
                        useCaches = false
                    }
                    if (conn.responseCode == 200) {
                        val text = conn.inputStream.bufferedReader().use { it.readText() }
                        val parsed = JSONObject(text)
                        if (parsed.has("ads") || parsed.has("adHeadline")) {
                            json = parsed
                            conn.disconnect()
                            break
                        }
                    }
                    conn.disconnect()
                } catch (e: Exception) {}
            }

            val finalJson = json
            if (finalJson == null) {
                if (manualUserCheck) {
                    runOnUiThread {
                        Toast.makeText(this, "Could not reach update server.", Toast.LENGTH_SHORT).show()
                    }
                }
                return@thread
            }

            val adMode = finalJson.optString("adMode", "hybrid")
            val adsArray = finalJson.optJSONArray("ads")

            val editor = prefs.edit()
                .putLong("last_sponsored_ad_sync_ms", now)
                .putString("ad_mode", adMode)

            if (adsArray != null && adsArray.length() > 0) {
                editor.putString("cached_four_ads_json", adsArray.toString())
            }
            editor.apply()

            preloadFourAdPictures()

            val remoteVersionCode = finalJson.optInt("versionCode", APP_RELEASE_NUMBER)
            val remoteVersionName = finalJson.optString("versionName", "1.1")
            val updateMsg = finalJson.optString(
                "updateMessage",
                "Version $remoteVersionName is ready! Tap below to download and install the update over your current app."
            )
            val apkUrl = finalJson.optString("apkDownloadUrl", DIRECT_APK_URL)
            val webUrl = finalJson.optString("websiteUrl", OFFICIAL_WEB_URL)

            runOnUiThread {
                if (needVersionCheck) {
                    prefs.edit().putLong("last_version_check_ms", now).apply()
                    if (remoteVersionCode > APP_RELEASE_NUMBER || manualUserCheck) {
                        tvUpdateTitle.text = if (remoteVersionCode > APP_RELEASE_NUMBER) {
                            "UPDATE v$remoteVersionName AVAILABLE!"
                        } else {
                            "LATEST VERSION ($remoteVersionName)"
                        }
                        tvUpdateMessage.text = updateMsg
                        btnUpdateNow.setOnClickListener {
                            updateOverlay.visibility = View.GONE
                            // Open direct APK download or website
                            openWebUrl(if (apkUrl.isNotEmpty()) apkUrl else webUrl)
                        }
                        updateOverlay.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun getFourSponsoredAdsList(): List<SponsoredAdItem> {
        val rawJson = prefs.getString("cached_four_ads_json", null)
        val list = mutableListOf<SponsoredAdItem>()

        if (!rawJson.isNullOrBlank()) {
            try {
                val arr = JSONArray(rawJson)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    list.add(
                        SponsoredAdItem(
                            id = obj.optInt("id", i + 1),
                            enabled = obj.optBoolean("enabled", true),
                            mediaType = obj.optString("mediaType", "image").lowercase(Locale.US),
                            mediaUrl = obj.optString("mediaUrl", "").trim(),
                            badge = obj.optString("adBadge", "SPONSORED • AD #${i + 1}"),
                            headline = obj.optString("adHeadline", "FixToCode Official"),
                            description = obj.optString("adDescription", ""),
                            buttonText = obj.optString("adButtonText", "VISIT OFFICIAL SITE ↗"),
                            targetUrl = obj.optString("adUrl", OFFICIAL_WEB_URL)
                        )
                    )
                }
            } catch (e: Exception) {}
        }

        if (list.isEmpty()) {
            list.add(
                SponsoredAdItem(
                    1, true, "image",
                    "https://images.unsplash.com/photo-1550745165-9bc0b252726f?auto=format&fit=crop&w=800&q=80",
                    "SPONSORED • AD #1", "FixToCode Official Channel",
                    "Explore high-performance apps, C++20 tools, and tech breakdowns by Rameez Ahmad.",
                    "VISIT OFFICIAL SITE ↗", OFFICIAL_WEB_URL
                )
            )
        }
        return list
    }

    private fun preloadFourAdPictures() {
        val ads = getFourSponsoredAdsList()
        for (ad in ads) {
            if (ad.enabled && ad.mediaType == "image" && ad.mediaUrl.startsWith("http") && !bitmapCache.containsKey(ad.mediaUrl)) {
                try {
                    val conn = (URL(ad.mediaUrl).openConnection() as HttpURLConnection).apply {
                        connectTimeout = 5000
                        readTimeout = 5000
                    }
                    if (conn.responseCode == 200) {
                        val bmp = BitmapFactory.decodeStream(conn.inputStream)
                        if (bmp != null) bitmapCache[ad.mediaUrl] = bmp
                    }
                    conn.disconnect()
                } catch (e: Exception) {}
            }
        }
    }

    private fun getNextSequentialSponsoredAd(): SponsoredAdItem {
        val allAds = getFourSponsoredAdsList()
        val enabledAds = allAds.filter { it.enabled }
        val pool = if (enabledAds.isNotEmpty()) enabledAds else allAds

        val seqIdx = prefs.getInt("sponsored_ad_seq_idx", 0)
        val chosen = pool[seqIdx % pool.size]
        prefs.edit().putInt("sponsored_ad_seq_idx", (seqIdx + 1) % pool.size).apply()
        return chosen
    }

    private fun openWebUrl(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        } catch (e: Exception) {}
    }

    private fun handleMatchEndWithAdCheck() {
        syncFourAdsAndCheckUpdate(forceCheckOnStartup = false, manualUserCheck = false)

        val plays = prefs.getInt("matches_played_count", 0) + 1
        prefs.edit().putInt("matches_played_count", plays).apply()
        currentAdMode = prefs.getString("ad_mode", "hybrid") ?: "hybrid"

        if (plays % 2 == 0) {
            if (currentAdMode == "custom") {
                showSequentialSponsoredAdThenVictory()
            } else if (UnityAdsBridge.isReady()) {
                UnityAdsBridge.showInterstitial(this, object : UnityAdsBridge.AdCompleteCallback {
                    override fun onAdFinished() {
                        runOnUiThread { showVictoryModal() }
                    }

                    override fun onAdFailed() {
                        runOnUiThread { showSequentialSponsoredAdThenVictory() }
                    }
                })
            } else {
                UnityAdsBridge.preloadAd("Interstitial_Android")
                showSequentialSponsoredAdThenVictory()
            }
        } else {
            UnityAdsBridge.preloadAd("Interstitial_Android")
            showVictoryModal()
        }
    }

    private fun showSequentialSponsoredAdThenVictory() {
        val ad = getNextSequentialSponsoredAd()

        adTimer?.cancel()
        stopAdMediaPlayback()

        isAdSkippable = false
        tvAdBadge.text = ad.badge
        tvAdHeadline.text = ad.headline
        tvAdDescription.text = ad.description
        btnAdAction.text = ad.buttonText
        currentAdTargetUrl = ad.targetUrl

        tvAdCountdown.text = "Skip in 6s"
        tvAdCountdown.setTextColor(0xFF38BDF8.toInt())
        adOverlay.visibility = View.VISIBLE

        if (ad.mediaType == "video" && ad.mediaUrl.startsWith("http")) {
            ivAdPicture.visibility = View.GONE
            tvAdFallbackIcon.visibility = View.GONE
            pbAdMediaLoading.visibility = View.VISIBLE
            vvAdVideo.visibility = View.VISIBLE

            try {
                vvAdVideo.setVideoURI(Uri.parse(ad.mediaUrl))
                vvAdVideo.setOnPreparedListener { mp ->
                    pbAdMediaLoading.visibility = View.GONE
                    mp.isLooping = true
                    vvAdVideo.start()
                }
                vvAdVideo.setOnErrorListener { _, _, _ ->
                    pbAdMediaLoading.visibility = View.GONE
                    vvAdVideo.visibility = View.GONE
                    tvAdFallbackIcon.visibility = View.VISIBLE
                    true
                }
            } catch (e: Exception) {
                pbAdMediaLoading.visibility = View.GONE
                vvAdVideo.visibility = View.GONE
                tvAdFallbackIcon.visibility = View.VISIBLE
            }
        } else if (ad.mediaUrl.startsWith("http")) {
            vvAdVideo.visibility = View.GONE
            val cachedBmp = bitmapCache[ad.mediaUrl]
            if (cachedBmp != null) {
                tvAdFallbackIcon.visibility = View.GONE
                pbAdMediaLoading.visibility = View.GONE
                ivAdPicture.setImageBitmap(cachedBmp)
                ivAdPicture.visibility = View.VISIBLE
            } else {
                ivAdPicture.visibility = View.GONE
                tvAdFallbackIcon.visibility = View.VISIBLE
                pbAdMediaLoading.visibility = View.VISIBLE
                thread(start = true) {
                    try {
                        val conn = (URL(ad.mediaUrl).openConnection() as HttpURLConnection).apply {
                            connectTimeout = 4500
                            readTimeout = 4500
                        }
                        if (conn.responseCode == 200) {
                            val bmp = BitmapFactory.decodeStream(conn.inputStream)
                            if (bmp != null) {
                                bitmapCache[ad.mediaUrl] = bmp
                                runOnUiThread {
                                    if (adOverlay.visibility == View.VISIBLE) {
                                        tvAdFallbackIcon.visibility = View.GONE
                                        pbAdMediaLoading.visibility = View.GONE
                                        ivAdPicture.setImageBitmap(bmp)
                                        ivAdPicture.visibility = View.VISIBLE
                                    }
                                }
                            }
                        }
                        conn.disconnect()
                    } catch (e: Exception) {
                        runOnUiThread { pbAdMediaLoading.visibility = View.GONE }
                    }
                }
            }
        } else {
            vvAdVideo.visibility = View.GONE
            ivAdPicture.visibility = View.GONE
            pbAdMediaLoading.visibility = View.GONE
            tvAdFallbackIcon.visibility = View.VISIBLE
        }

        adTimer = object : CountDownTimer(6000L, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                val secondsLeft = ((millisUntilFinished + 999L) / 1000L).toInt().coerceAtLeast(1)
                tvAdCountdown.text = "Skip in ${secondsLeft}s"
            }

            override fun onFinish() {
                isAdSkippable = true
                tvAdCountdown.text = "SKIP AD ⏭"
                tvAdCountdown.setTextColor(0xFFF59E0B.toInt())
            }
        }.start()
    }

    private fun stopAdMediaPlayback() {
        adTimer?.cancel()
        try {
            if (vvAdVideo.isPlaying) {
                vvAdVideo.stopPlayback()
            }
        } catch (e: Exception) {}
    }

    private fun getUnlockedLevel(): Int {
        return prefs.getInt("unlocked_level", 1).coerceIn(1, 30)
    }

    private fun getTotalBankPoints(): Int {
        return prefs.getInt("total_bank_pts", 0)
    }

    private fun setupLevelSelectorBar() {
        levelOptionsLayout.removeAllViews()
        val unlocked = getUnlockedLevel()
        tvTotalBank.text = "🏆 ${getTotalBankPoints()} PTS"

        LevelRepository.levels.forEach { spec ->
            val isUnlocked = spec.number <= unlocked
            val isSelected = spec.number == selectedLevel
            val isCleared = spec.number < unlocked

            val btn = Button(this).apply {
                text = when {
                    !isUnlocked -> "🔒 L${spec.number}"
                    isCleared -> "★ L${spec.number}"
                    else -> "LVL ${spec.number}"
                }
                textSize = 12f
                isEnabled = isUnlocked
                setTextColor(
                    when {
                        isSelected -> Color.BLACK
                        !isUnlocked -> 0xFF475569.toInt()
                        else -> Color.WHITE
                    }
                )
                setBackgroundColor(
                    when {
                        isSelected -> 0xFF38BDF8.toInt()
                        !isUnlocked -> 0xFF0E131D.toInt()
                        else -> 0xFF121824.toInt()
                    }
                )
                setOnClickListener {
                    if (isUnlocked) {
                        selectedLevel = spec.number
                        setupLevelSelectorBar()
                        updateLevelBannerText(spec.number)
                    }
                }
            }
            levelOptionsLayout.addView(btn)
        }
    }

    private fun updateLevelBannerText(levelNum: Int) {
        val spec = LevelRepository.levels[(levelNum - 1).coerceIn(0, 29)]
        tvLevelInfo.text = "LVL ${spec.number}: ${spec.title} • ${spec.pointsPerBox} pts/box • Win Bonus: +${spec.winBonus}"
    }

    private fun highlightSelectedGridButton() {
        for (i in 0 until gridOptionsLayout.childCount) {
            val b = gridOptionsLayout.getChildAt(i) as Button
            val preset = gridPresets.getOrNull(i)
            val match = preset?.second?.first == selectedRows && preset.second.second == selectedCols
            b.setBackgroundColor(if (match) 0xFF38BDF8.toInt() else 0xFF121824.toInt())
            b.setTextColor(if (match) Color.BLACK else Color.WHITE)
        }
    }

    private fun isTablet(): Boolean {
        val screenLayout = resources.configuration.screenLayout
        val size = screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK
        return size >= Configuration.SCREENLAYOUT_SIZE_LARGE
    }

    private fun setupAdaptiveGridOptions() {
        val tvDevice = findViewById<TextView>(R.id.tvDeviceType)
        gridOptionsLayout.removeAllViews()

        gridPresets = if (isTablet()) {
            tvDevice.text = "TABLET HARDWARE DETECTED"
            listOf(
                "3x3" to Pair(3, 3),
                "4x4" to Pair(4, 4),
                "4x5" to Pair(5, 4),
                "5x5" to Pair(5, 5),
                "5x7" to Pair(7, 5),
                "5x8" to Pair(8, 5),
                "6x6" to Pair(6, 6),
                "8x8" to Pair(8, 8)
            )
        } else {
            tvDevice.text = "SMARTPHONE DISPLAY OPTIMIZED"
            listOf(
                "3x3" to Pair(3, 3),
                "4x4" to Pair(4, 4),
                "4x5" to Pair(5, 4),
                "5x5" to Pair(5, 5),
                "5x7" to Pair(7, 5),
                "5x8" to Pair(8, 5)
            )
        }

        gridPresets.forEach { pair ->
            val isSelected = (pair.second.first == selectedRows && pair.second.second == selectedCols)
            val btn = Button(this).apply {
                text = pair.first
                textSize = 12f
                setTextColor(if (isSelected) Color.BLACK else Color.WHITE)
                setBackgroundColor(if (isSelected) 0xFF38BDF8.toInt() else 0xFF121824.toInt())
                setOnClickListener {
                    selectedRows = pair.second.first
                    selectedCols = pair.second.second
                    prefs.edit()
                        .putInt("user_pref_rows", selectedRows)
                        .putInt("user_pref_cols", selectedCols)
                        .apply()
                    highlightSelectedGridButton()
                }
            }
            gridOptionsLayout.addView(btn)
        }
    }

    private fun setupPlayerCountTabs() {
        playerCountTabsLayout.removeAllViews()
        val counts = listOf(2, 3, 4, 5)

        counts.forEach { count ->
            val isSelected = (count == playerCount)
            val btn = Button(this).apply {
                text = "${count}P"
                textSize = 13f
                val lp = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, 1f)
                lp.setMargins(4, 0, 4, 0)
                layoutParams = lp
                setTextColor(if (isSelected) Color.BLACK else Color.WHITE)
                setBackgroundColor(if (isSelected) 0xFF38BDF8.toInt() else 0xFF121824.toInt())
                setOnClickListener {
                    saveRosterInputsToPrefs()
                    playerCount = count
                    prefs.edit().putInt("user_pref_player_count", playerCount).apply()
                    for (i in 0 until playerCountTabsLayout.childCount) {
                        val b = playerCountTabsLayout.getChildAt(i) as Button
                        b.setBackgroundColor(0xFF121824.toInt())
                        b.setTextColor(Color.WHITE)
                    }
                    setBackgroundColor(0xFF38BDF8.toInt())
                    setTextColor(Color.BLACK)
                    renderRoster()
                }
            }
            playerCountTabsLayout.addView(btn)
        }
    }

    /**
     * Renders every player row with a real Android ON/OFF Switch in front of the player
     * to turn AI ON ("AI: ON") or OFF ("AI: OFF").
     */
    private fun renderRoster() {
        rosterLayout.removeAllViews()
        activePlayers.clear()

        for (i in 0 until playerCount) {
            val color = playerColors[i]
            val defaultBotState = (i >= 1)
            val isBot = prefs.getBoolean("user_pref_bot_p$i", defaultBotState)
            val defaultName = if (i == 0) "Player 1" else "Player ${i + 1}"
            val savedName = prefs.getString("user_pref_name_p$i", defaultName) ?: defaultName

            val player = Player(i, savedName, color, isBot)
            activePlayers.add(player)

            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(14, 10, 14, 10)
                setBackgroundColor(0xFF121824.toInt())
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 6, 0, 6) }
                layoutParams = lp
            }

            val colorBadge = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(26, 26).apply { setMargins(4, 0, 14, 0) }
                setBackgroundColor(color)
            }

            val etName = EditText(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                setText(savedName)
                setTextColor(Color.WHITE)
                textSize = 13f
                setBackgroundColor(Color.TRANSPARENT)
            }

            // Real Sliding ON/OFF Switch in front of every player to toggle AI!
            val aiSwitch = Switch(this).apply {
                isChecked = player.isBot
                text = if (player.isBot) "AI: ON  " else "AI: OFF  "
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
                setTextColor(if (player.isBot) 0xFF38BDF8.toInt() else 0xFF94A3B8.toInt())
                setOnCheckedChangeListener { _, isChecked ->
                    player.isBot = isChecked
                    prefs.edit().putBoolean("user_pref_bot_p$i", isChecked).apply()
                    text = if (isChecked) "AI: ON  " else "AI: OFF  "
                    setTextColor(if (isChecked) 0xFF38BDF8.toInt() else 0xFF94A3B8.toInt())
                }
            }

            row.addView(colorBadge)
            row.addView(etName)
            row.addView(aiSwitch)
            rosterLayout.addView(row)
        }
    }

    private fun saveRosterInputsToPrefs() {
        val editor = prefs.edit()
        for (i in 0 until rosterLayout.childCount) {
            val row = rosterLayout.getChildAt(i) as? LinearLayout ?: continue
            val etName = row.getChildAt(1) as? EditText ?: continue
            val inputName = etName.text.toString().trim()
            if (inputName.isNotEmpty() && i < activePlayers.size) {
                activePlayers[i].name = inputName
                editor.putString("user_pref_name_p$i", inputName)
            }
        }
        editor.apply()
    }

    private fun startMatch() {
        saveRosterInputsToPrefs()

        gameView.audio.isEnabled = switchSound.isChecked
        gameView.isVibrationEnabled = switchVibration.isChecked

        stopAdMediaPlayback()
        adOverlay.visibility = View.GONE
        winnerOverlay.visibility = View.GONE
        configContainer.visibility = View.GONE
        arenaContainer.visibility = View.VISIBLE

        activePlayers.forEach {
            it.score = 0
            it.boxesCount = 0
            it.totalTimeMillis = 0L
            it.movesCount = 0
        }

        val spec = LevelRepository.levels[(selectedLevel - 1).coerceIn(0, 29)]
        val engine = GameEngine(
            rows = selectedRows,
            cols = selectedCols,
            players = activePlayers,
            pointsPerBox = spec.pointsPerBox,
            aiSkillLevel = spec.aiSkillLevel
        )
        gameView.engine = engine
        engine.startTurnClock()
        gameView.invalidate()

        updateArenaHud()

        if (engine.getCurrentPlayer().isBot) {
            gameView.postDelayed({ gameView.runBotTurn() }, 400L)
        }
    }

    private fun returnToConfig() {
        stopAdMediaPlayback()
        adOverlay.visibility = View.GONE
        winnerOverlay.visibility = View.GONE
        arenaContainer.visibility = View.GONE
        configContainer.visibility = View.VISIBLE
        setupLevelSelectorBar()
        highlightSelectedGridButton()
    }

    private fun updateArenaHud() {
        val eng = gameView.engine ?: return
        val current = eng.getCurrentPlayer()
        tvTurnIndicator.text = "LVL $selectedLevel • ${current.name}'s Turn"
        tvTurnIndicator.setTextColor(current.color)

        val totalHumanMillis = eng.players.filter { !it.isBot }.sumOf { it.totalTimeMillis }

        arenaScoresLayout.removeAllViews()
        eng.players.forEach { p ->
            val tv = TextView(this).apply {
                val baseText = "${p.name}: ${p.score}"
                text = if (switchSpeedAnalytics.isChecked && !p.isBot && totalHumanMillis > 0) {
                    val pct = (p.totalTimeMillis.toDouble() / totalHumanMillis.toDouble()) * 100.0
                    "$baseText (${String.format(Locale.US, "%.0f", pct)}% time)  "
                } else {
                    "$baseText  "
                }
                textSize = 12f
                setTextColor(if (p.id == current.id) p.color else Color.LTGRAY)
                setPadding(14, 4, 14, 4)
            }
            arenaScoresLayout.addView(tv)
        }
    }

    private fun showVictoryModal() {
        val eng = gameView.engine ?: return
        val sorted = eng.players.sortedByDescending { it.score }
        val winner = sorted.firstOrNull() ?: return
        val spec = LevelRepository.levels[(selectedLevel - 1).coerceIn(0, 29)]
        val hasBotOpponent = eng.players.any { it.isBot }

        if (winner.id == 0 && hasBotOpponent) {
            val newBank = getTotalBankPoints() + winner.score + spec.winBonus
            val unlocked = getUnlockedLevel()
            val nextLevel = (selectedLevel + 1).coerceAtMost(30)

            prefs.edit()
                .putInt("total_bank_pts", newBank)
                .putInt("unlocked_level", maxOf(unlocked, nextLevel))
                .apply()

            tvWinnerTitle.text = "LEVEL $selectedLevel CLEARED!"
            tvWinnerTitle.setTextColor(winner.color)
            tvWinnerSubtitle.text = "Score: ${winner.score} + ${spec.winBonus} Bonus  •  Bank: 🏆 $newBank"

            if (selectedLevel < 30) {
                btnOverlayPlayAgain.text = "NEXT: LEVEL ${selectedLevel + 1} ⏭"
                btnOverlayPlayAgain.setOnClickListener {
                    winnerOverlay.visibility = View.GONE
                    selectedLevel = (selectedLevel + 1).coerceAtMost(30)
                    setupLevelSelectorBar()
                    updateLevelBannerText(selectedLevel)
                    startMatch()
                }
            } else {
                btnOverlayPlayAgain.text = "PLAY LEVEL 30 AGAIN"
                btnOverlayPlayAgain.setOnClickListener {
                    winnerOverlay.visibility = View.GONE
                    startMatch()
                }
            }
        } else {
            tvWinnerTitle.text = "${winner.name.uppercase()} WINS!"
            tvWinnerTitle.setTextColor(winner.color)
            tvWinnerSubtitle.text = "Captured ${winner.boxesCount} boxes (${winner.score} pts)"

            btnOverlayPlayAgain.text = if (hasBotOpponent) "RETRY LEVEL $selectedLevel" else "PLAY AGAIN"
            btnOverlayPlayAgain.setOnClickListener {
                winnerOverlay.visibility = View.GONE
                startMatch()
            }
        }

        winnerStatsLayout.removeAllViews()
        sorted.forEachIndexed { rank, p ->
            val item = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                setPadding(14, 10, 14, 10)
                setBackgroundResource(R.drawable.bg_stat_item)
                val lp = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { setMargins(0, 4, 0, 4) }
                layoutParams = lp
            }

            val rankBadge = TextView(this).apply {
                text = "#${rank + 1} "
                setTextColor(Color.WHITE)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
            }

            val dot = View(this).apply {
                layoutParams = LinearLayout.LayoutParams(18, 18).apply { setMargins(6, 6, 10, 0) }
                setBackgroundColor(p.color)
            }

            val nameView = TextView(this).apply {
                text = p.name + if (p.isBot) " (AI)" else ""
                setTextColor(Color.WHITE)
                textSize = 12f
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }

            val scoreView = TextView(this).apply {
                text = "${p.boxesCount} boxes (${p.score} pts)"
                setTextColor(p.color)
                textSize = 12f
                typeface = Typeface.DEFAULT_BOLD
            }

            item.addView(rankBadge)
            item.addView(dot)
            item.addView(nameView)
            item.addView(scoreView)
            winnerStatsLayout.addView(item)
        }

        winnerAnalyticsLayout.removeAllViews()
        if (switchSpeedAnalytics.isChecked) {
            tvAnalyticsHeading.visibility = View.VISIBLE
            winnerAnalyticsLayout.visibility = View.VISIBLE

            val humanPlayers = eng.players.filter { !it.isBot }
            val totalHumanMillis = humanPlayers.sumOf { it.totalTimeMillis }

            val rankedBySpeed = humanPlayers.sortedBy { it.getAverageResponseSec() }
            val fastestHuman = rankedBySpeed.firstOrNull { it.movesCount > 0 }

            humanPlayers.forEach { p ->
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    setPadding(14, 8, 14, 8)
                    setBackgroundResource(R.drawable.bg_stat_item)
                    val lp = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { setMargins(0, 3, 0, 3) }
                    layoutParams = lp
                }

                val pct = if (totalHumanMillis > 0) (p.totalTimeMillis.toDouble() / totalHumanMillis.toDouble()) * 100.0 else 0.0
                val avgSec = p.getAverageResponseSec()

                val nameLabel = TextView(this).apply {
                    val badge = if (p.id == fastestHuman?.id && humanPlayers.size > 1) " ⚡ FASTEST" else ""
                    text = "${p.name}$badge"
                    setTextColor(if (p.id == fastestHuman?.id && humanPlayers.size > 1) 0xFF38BDF8.toInt() else Color.WHITE)
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                }

                val metricsLabel = TextView(this).apply {
                    text = "${String.format(Locale.US, "%.1f", avgSec)}s avg  |  ${String.format(Locale.US, "%.0f", pct)}% time"
                    setTextColor(p.color)
                    textSize = 11f
                    typeface = Typeface.DEFAULT_BOLD
                }

                row.addView(nameLabel)
                row.addView(metricsLabel)
                winnerAnalyticsLayout.addView(row)
            }
        } else {
            tvAnalyticsHeading.visibility = View.GONE
            winnerAnalyticsLayout.visibility = View.GONE
        }

        winnerOverlay.visibility = View.VISIBLE
    }

    override fun onDestroy() {
        stopAdMediaPlayback()
        super.onDestroy()
    }
}