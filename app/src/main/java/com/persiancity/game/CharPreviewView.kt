package com.persiancity.game

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import com.persiancity.game.game.SpriteLib

/**
 * پیش‌نمایش بزرگ شخصیت در صفحه ساخت شخصیت
 * سمت چپ: کاراکتر بزرگ با انیمیشن راه رفتن ملایم
 */
class CharPreviewView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    var gender: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    var hairStyle: Int = 0
        set(value) {
            field = value
            invalidate()
        }

    private val sprites = SpriteLib()
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var animTime = 0f
    private var running = true

    private val ticker = object : Runnable {
        override fun run() {
            if (!running) return
            animTime += 0.05f
            invalidate()
            postDelayed(this, 50)
        }
    }

    init {
        post(ticker)
    }

    override fun onDetachedFromWindow() {
        running = false
        removeCallbacks(ticker)
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val w = width.toFloat()
        val h = height.toFloat()
        if (w <= 0 || h <= 0) return

        paint.color = Color.WHITE
        canvas.drawRoundRect(8f, 8f, w - 8f, h - 8f, 40f, 40f, paint)
        paint.color = 0xFF81D4FA.toInt()
        canvas.drawRoundRect(16f, 16f, w - 16f, h * 0.72f, 32f, 32f, paint)
        paint.color = 0xFF8BC34A.toInt()
        canvas.drawRoundRect(16f, h * 0.62f, w - 16f, h - 16f, 32f, 32f, paint)

        paint.color = 0xFFFFD54F.toInt()
        canvas.drawCircle(w - 70f, 80f, 36f, paint)

        paint.color = 0xFFFFFFFF.toInt()
        canvas.drawOval(40f, 70f, 150f, 115f, paint)
        canvas.drawOval(90f, 55f, 200f, 105f, paint)

        val scale = (h / 400f).coerceIn(1.4f, 3.2f)
        val cx = w / 2f
        val cy = h * 0.62f
        canvas.save()
        canvas.translate(cx, cy)
        canvas.scale(scale, scale)

        val outfit = if (gender == 1) SpriteLib.OUTFITS[5] else SpriteLib.OUTFITS[0]
        val hair = if (gender == 1) 2 else hairStyle
        sprites.drawPerson(
            canvas, 0f, 0f, 0, animTime,
            outfit[0], outfit[1], 0xFFF7C99B.toInt(), 0xFF3E2723.toInt(), hair,
            -1, true, gender
        )
        canvas.restore()
    }
}
