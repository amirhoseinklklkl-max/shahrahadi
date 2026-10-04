package com.persiancity.game

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.persiancity.game.game.GameView

/**
 * اکتیویتی اصلی بازی — نمای تمام‌صفحه افقی را می‌سازد
 * بار اول: صفحه ساخت شخصیت (اسم + جنسیت با پیش‌نمایش بزرگ)
 * بعد از آن: مستقیم ادامه بازی ذخیره‌شده
 */
class GameActivity : AppCompatActivity() {

    private var gameView: GameView? = null
    private var selectedGender = 0

    // ✅ سینما: پخش واقعی ویدیو (تام و جری) با WebView
    private var rootLayout: android.widget.FrameLayout? = null
    private var videoOverlay: android.view.View? = null
    private var cinemaWebView: android.webkit.WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            SoundManager.init(this)

            window.setFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN
            )

            val prefs = getSharedPreferences("shahrshadi_prefs", Context.MODE_PRIVATE)
            val hasCharacter = prefs.contains("charName")

            if (!hasCharacter && !SaveManager.hasSave(this)) {
                showCharacterCreation()
            } else {
                startGameView()
            }
        } catch (t: Throwable) {
            saveCrashLog(t)
            android.widget.Toast.makeText(
                this,
                "متأسفانه بازی شروع نشد. لطفاً دوباره تلاش کن.",
                android.widget.Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    // ================= صفحه ساخت شخصیت =================

    private fun showCharacterCreation() {
        setContentView(R.layout.character_create)

        val preview = findViewById<CharPreviewView>(R.id.charPreview)
        val etName = findViewById<android.widget.EditText>(R.id.etName)
        val btnBoy = findViewById<android.widget.Button>(R.id.btnBoy)
        val btnGirl = findViewById<android.widget.Button>(R.id.btnGirl)
        val btnStart = findViewById<android.widget.Button>(R.id.btnStartCreate)

        fun highlightGender() {
            btnBoy.isSelected = selectedGender == 0
            btnGirl.isSelected = selectedGender == 1
            btnBoy.setTextColor(if (selectedGender == 0) 0xFFE65100.toInt() else 0xFF4E342E.toInt())
            btnGirl.setTextColor(if (selectedGender == 1) 0xFFE65100.toInt() else 0xFF4E342E.toInt())
        }

        btnBoy.setOnClickListener {
            selectedGender = 0
            preview.gender = 0
            preview.hairStyle = 0
            SoundManager.play("click")
            highlightGender()
        }
        btnGirl.setOnClickListener {
            selectedGender = 1
            preview.gender = 1
            preview.hairStyle = 2
            SoundManager.play("click")
            highlightGender()
        }
        highlightGender()

        btnStart.setOnClickListener {
            val name = etName.text.toString().trim().ifEmpty { "قهرمان" }
            val prefs = getSharedPreferences("shahrshadi_prefs", Context.MODE_PRIVATE)
            prefs.edit()
                .putString("charName", name)
                .putInt("charGender", selectedGender)
                .apply()
            SoundManager.play("success")
            startGameView()
        }
    }

    // ================= شروع خود بازی =================

    private fun startGameView() {
        gameView = GameView(this)

        // ✅ سینما: میزبان پخش ویدیوی واقعی (تام و جری از فیلو)
        gameView!!.cinemaHost = object : GameView.CinemaHost {
            override fun openCinemaVideo(url: String) {
                openCinemaOverlay(url)
            }

            override fun closeCinemaVideo() {
                closeCinemaOverlay()
            }
        }

        gameView!!.setKeepScreenOn(true)
        // لایهٔ ریشه: GameView + پوشش ویدیوی سینما روی آن
        val root = android.widget.FrameLayout(this)
        root.addView(
            gameView, android.widget.FrameLayout.LayoutParams(
                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                android.view.ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
        rootLayout = root
        setContentView(root)
        hideSystemUi()
    }

    // ================= سینما: پخش ویدیوی واقعی 🎬 =================

    /**
     * نمایش ویدیوی واقعی سینما (تام و جری) در WebView تمام‌صفحه
     * + دکمهٔ «خروج» وسط پایین (هماهنگ با بازی)
     */
    private fun openCinemaOverlay(url: String) {
        runOnUiThread {
            val root = rootLayout ?: return@runOnUiThread
            if (videoOverlay != null) return@runOnUiThread

            val overlay = android.widget.FrameLayout(this)
            overlay.setBackgroundColor(0xFF000000.toInt())

            val wv = android.webkit.WebView(this)
            wv.settings.javaScriptEnabled = true
            wv.settings.domStorageEnabled = true
            wv.settings.mediaPlaybackRequiresUserGesture = false
            wv.settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            wv.setBackgroundColor(0xFF000000.toInt())
            wv.webChromeClient = object : android.webkit.WebChromeClient() {}
            wv.loadUrl(url)
            overlay.addView(
                wv, android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            cinemaWebView = wv

            // دکمهٔ خروج از سینما — وسط پایین صفحه
            val btn = android.widget.Button(this)
            btn.text = "✕ خروج از سینما"
            btn.setTextColor(0xFFFFFFFF.toInt())
            btn.textSize = 20f
            btn.isAllCaps = false
            val bg = android.graphics.drawable.GradientDrawable()
            bg.setColor(0xE5E53935.toInt())
            bg.cornerRadius = 30f
            btn.background = bg
            btn.setPadding(60, 24, 60, 24)
            btn.setOnClickListener {
                SoundManager.play("click")
                gameView?.stopMovie()
            }
            val blp = android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT,
                android.widget.FrameLayout.LayoutParams.WRAP_CONTENT
            )
            blp.gravity = android.view.Gravity.BOTTOM or android.view.Gravity.CENTER_HORIZONTAL
            blp.bottomMargin = 28
            overlay.addView(btn, blp)

            root.addView(
                overlay, android.widget.FrameLayout.LayoutParams(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )
            )
            videoOverlay = overlay
            // توقف حلقهٔ بازی حین تماشای فیلم
            gameView?.pauseGame()
        }
    }

    private fun closeCinemaOverlay() {
        runOnUiThread {
            videoOverlay?.let { rootLayout?.removeView(it) }
            cinemaWebView?.let { wv ->
                try {
                    wv.stopLoading()
                    wv.loadUrl("about:blank")
                    wv.onPause()
                    wv.destroy()
                } catch (_: Throwable) {
                }
            }
            cinemaWebView = null
            videoOverlay = null
            gameView?.resumeGame()
        }
    }

    private fun isCinemaOpen(): Boolean = videoOverlay != null

    private fun saveCrashLog(t: Throwable) {
        try {
            java.io.File(filesDir, "crash_log.txt").writeText(
                android.util.Log.getStackTraceString(t)
            )
        } catch (_: Exception) {
        }
    }

    private fun hideSystemUi() {
        val decor = window.decorView
        decor.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUi()
    }

    override fun onPause() {
        super.onPause()
        gameView?.pauseGame()
        SoundManager.stopMusic()
    }

    override fun onResume() {
        super.onResume()
        gameView?.resumeGame()
        SoundManager.startMusic()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (isCinemaOpen()) {
            // حین پخش ویدیو، دکمهٔ برگشت = خروج از سینما
            gameView?.stopMovie()
        } else {
            gameView?.openPauseMenu()
        }
    }
}
