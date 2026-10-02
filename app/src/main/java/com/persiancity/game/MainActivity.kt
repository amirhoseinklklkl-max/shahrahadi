package com.persiancity.game

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity

/**
 * صفحه شروع بازی «شهر شادی»
 * منوی اصلی با دکمه‌های شروع بازی جدید، ادامه بازی، راهنما و صدا
 */
class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_main)

        val btnNew = findViewById<View>(R.id.btnNewGame)
        val btnContinue = findViewById<View>(R.id.btnContinue)
        val btnHelp = findViewById<View>(R.id.btnHelp)
        val btnSound = findViewById<View>(R.id.btnSound)
        val btnExit = findViewById<View>(R.id.btnExit)

        // اگر بازی ذخیره‌شده وجود دارد، دکمه «ادامه بازی» نمایش داده شود
        val hasSave = SaveManager.hasSave(this)
        btnContinue.visibility = if (hasSave) View.VISIBLE else View.GONE

        SoundManager.init(this)
        updateSoundLabel(btnSound)

        btnNew.setOnClickListener {
            if (SaveManager.hasSave(this)) {
                AlertDialog.Builder(this)
                    .setTitle(getString(R.string.game_title))
                    .setMessage(getString(R.string.confirm_new_game))
                    .setPositiveButton(getString(R.string.yes)) { _, _ ->
                        SaveManager.deleteSave(this)
                        openGame()
                    }
                    .setNegativeButton(getString(R.string.no), null)
                    .show()
            } else {
                openGame()
            }
        }

        btnContinue.setOnClickListener { openGame() }

        btnHelp.setOnClickListener {
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
        btn.text = if (SoundManager.isMuted) getString(R.string.sound_off) else getString(R.string.sound_on)
    }

    private fun openGame() {
        startActivity(Intent(this, GameActivity::class.java))
        finish()
    }
}
