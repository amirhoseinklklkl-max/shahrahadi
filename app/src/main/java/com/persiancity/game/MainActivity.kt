package com.persiancity.game

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

/**
 * صفحه شروع بازی «شهر شادی»
 * بازی همیشه ادامه بازی قبلی است (ذخیره خودکار)؛ دکمه «شروع بازی» مستقیم وارد می‌شود.
 * بار اول، صفحه ساخت شخصیت (اسم + جنسیت) در GameActivity نمایش داده می‌شود.
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)

        val btnStart = findViewById<View>(R.id.btnNewGame)
        val btnHelp = findViewById<View>(R.id.btnHelp)
        val btnSound = findViewById<View>(R.id.btnSound)
        val btnExit = findViewById<View>(R.id.btnExit)

        SoundManager.init(this)
        updateSoundLabel(btnSound)

        btnStart.setOnClickListener {
            SoundManager.play("click")
            openGame()
        }

        btnHelp.setOnClickListener {
            SoundManager.play("click")
            AlertDialog.Builder(this)
                .setTitle(getString(R.string.help_title))
                .setMessage(getString(R.string.help_text))
                .setPositiveButton(getString(R.string.ok), null)
                .show()
        }

        btnSound.setOnClickListener {
            val newMuted = !SoundManager.isMuted
            SoundManager.setMuted(this, newMuted)
            updateSoundLabel(btnSound)
        }

        btnExit.setOnClickListener { finishAffinity() }
    }

    private fun updateSoundLabel(view: View) {
        val btn = view as android.widget.Button
        btn.text = if (SoundManager.isMuted) getString(R.string.btn_sound_off) else getString(R.string.btn_sound_on)
    }

    private fun openGame() {
        // finish() صدا نمی‌زنیم؛ اگر بازی خطایی داشت، کاربر به همین منو برمی‌گردد
        startActivity(Intent(this, GameActivity::class.java))
    }
}
