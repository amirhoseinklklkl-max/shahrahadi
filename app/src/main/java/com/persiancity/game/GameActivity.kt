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
        gameView!!.setKeepScreenOn(true)
        setContentView(gameView)
        hideSystemUi()
    }

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
        gameView?.openPauseMenu()
    }
}
