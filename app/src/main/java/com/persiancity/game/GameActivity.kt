package com.persiancity.game

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.persiancity.game.game.GameView

/**
 * اکتیویتی اصلی بازی — نمای تمام‌صفحه افقی را می‌سازد
 */
class GameActivity : AppCompatActivity() {

    private var gameView: GameView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        SoundManager.init(this)

        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        gameView = GameView(this)
        gameView!!.setKeepScreenOn(true)
        setContentView(gameView)
        hideSystemUi()
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
    }

    override fun onResume() {
        super.onResume()
        gameView?.resumeGame()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        // دکمه برگشت = توقف بازی (نه خروج مستقیم)
        gameView?.openPauseMenu()
    }
}
