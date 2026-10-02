package com.persiancity.game

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.SoundPool
import java.io.File
import java.io.FileOutputStream
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

/**
 * افکت‌های صوتی بازی — همه صداها با کد ساخته می‌شوند (بدون فایل صوتی)
 * صدای موتور ماشین هم به صورت زنده و بر اساس سرعت تغییر می‌کند.
 */
object SoundManager {

    private lateinit var soundPool: SoundPool
    private val soundIds = HashMap<String, Int>()
    private var prefs: SharedPreferences? = null
    private var appContext: Context? = null

    @get:JvmStatic
    var isMuted: Boolean = false
        private set

    // موتور ماشین
    private var engineTrack: AudioTrack? = null
    private var engineThread: Thread? = null
    @Volatile
    private var engineRunning = false
    @Volatile
    private var engineIntensity = 0f

    private const val SAMPLE_RATE = 22050

    fun init(context: Context) {
        if (soundIds.isNotEmpty()) return
        appContext = context.applicationContext
        prefs = context.getSharedPreferences("shahrshadi_prefs", Context.MODE_PRIVATE)
        isMuted = prefs?.getBoolean("muted", false) ?: false

        soundPool = SoundPool.Builder()
            .setMaxStreams(6)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .build()

        // ساخت فایل‌های صوتی کوچک با کد
        val dir = File(context.cacheDir, "sounds")
        dir.mkdirs()
        putSound(dir, "click") { tone(880.0, 0.06, 0.5, square = true) }
        putSound(dir, "coin") { concat(tone(988.0, 0.09, 0.5), tone(1319.0, 0.16, 0.5)) }
        putSound(dir, "buy") { concat(tone(660.0, 0.07, 0.5), tone(990.0, 0.07, 0.5), tone(1320.0, 0.14, 0.5)) }
        putSound(dir, "success") {
            concat(tone(523.0, 0.10, 0.5), tone(659.0, 0.10, 0.5), tone(784.0, 0.10, 0.5), tone(1047.0, 0.22, 0.5))
        }
        putSound(dir, "door") { slide(220.0, 110.0, 0.14, 0.5) }
        putSound(dir, "eat") { concat(tone(392.0, 0.08, 0.4), tone(523.0, 0.10, 0.4)) }
        putSound(dir, "horn") {
            mix(tone(350.0, 0.35, 0.4, square = true), tone(440.0, 0.35, 0.4, square = true))
        }
        putSound(dir, "mission") {
            concat(tone(784.0, 0.09, 0.5), tone(988.0, 0.09, 0.5), tone(1175.0, 0.09, 0.5), tone(1568.0, 0.25, 0.5))
        }
    }

    private fun putSound(dir: File, name: String, gen: () -> ShortArray) {
        try {
            val f = File(dir, "$name.wav")
            if (!f.exists()) writeWav(f, gen())
            val id = soundPool.load(f.absolutePath, 1)
            soundIds[name] = id
        } catch (_: Exception) {
        }
    }

    @JvmStatic
    fun setMuted(context: Context, muted: Boolean) {
        isMuted = muted
        prefs?.edit()?.putBoolean("muted", muted)?.apply()
        if (muted) stopEngine()
    }

    @JvmStatic
    fun play(name: String) {
        if (isMuted) return
        val id = soundIds[name] ?: return
        soundPool.play(id, 0.9f, 0.9f, 1, 0, 1f)
    }

    // ------------------- صدای زنده موتور -------------------

    @JvmStatic
    fun startEngine() {
        if (isMuted || engineRunning) return
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            engineTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            engineTrack?.play()
            engineRunning = true
            engineThread = Thread {
                var phase = 0.0
                val buf = ShortArray(1024)
                var smoothFreq = 60.0
                while (engineRunning) {
                    val freq = 55.0 + engineIntensity * 130.0
                    smoothFreq = smoothFreq * 0.85 + freq * 0.15
                    val vol = 0.16 + engineIntensity * 0.14
                    for (i in buf.indices) {
                        phase += 2 * PI * smoothFreq / SAMPLE_RATE
                        if (phase > 2 * PI) phase -= 2 * PI
                        // موج دندانه اره‌ای نرم = صدای موتور
                        val saw = 2.0 * (phase / (2 * PI)) - 1.0
                        val soft = 0.6 * sin(phase) + 0.4 * saw
                        buf[i] = (soft * vol * Short.MAX_VALUE).toInt().toShort()
                    }
                    engineTrack?.write(buf, 0, buf.size)
                }
            }
            engineThread?.start()
        } catch (_: Exception) {
        }
    }

    @JvmStatic
    fun setEngineIntensity(i: Float) {
        engineIntensity = min(abs(i), 1f)
    }

    @JvmStatic
    fun stopEngine() {
        engineRunning = false
        try {
            engineThread?.join(300)
            engineTrack?.stop()
            engineTrack?.release()
        } catch (_: Exception) {
        }
        engineTrack = null
        engineThread = null
    }

    // ------------------- ساخت موج صوتی -------------------

    private fun tone(freq: Double, dur: Double, vol: Double, square: Boolean = false): ShortArray {
        val n = (SAMPLE_RATE * dur).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SAMPLE_RATE
            val env = 1.0 - (i.toDouble() / n) * 0.7
            var v = sin(2 * PI * freq * t)
            if (square) v = if (v >= 0) 0.7 else -0.7
            out[i] = (v * env * vol * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun slide(f1: Double, f2: Double, dur: Double, vol: Double): ShortArray {
        val n = (SAMPLE_RATE * dur).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val p = i.toDouble() / n
            val f = f1 + (f2 - f1) * p
            phase += 2 * PI * f / SAMPLE_RATE
            out[i] = (sin(phase) * (1 - p) * vol * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun concat(vararg parts: ShortArray): ShortArray {
        var size = 0
        for (p in parts) size += p.size
        val out = ShortArray(size)
        var off = 0
        for (p in parts) {
            System.arraycopy(p, 0, out, off, p.size)
            off += p.size
        }
        return out
    }

    private fun mix(a: ShortArray, b: ShortArray): ShortArray {
        val n = maxOf(a.size, b.size)
        val out = ShortArray(n)
        for (i in 0 until n) {
            val va = if (i < a.size) a[i].toDouble() / Short.MAX_VALUE else 0.0
            val vb = if (i < b.size) b[i].toDouble() / Short.MAX_VALUE else 0.0
            out[i] = (((va + vb) / 2.0) * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun writeWav(f: File, samples: ShortArray) {
        val dataLen = samples.size * 2
        val fos = FileOutputStream(f)
        val header = ByteArray(44)
        fun putInt(off: Int, v: Int) {
            header[off] = (v and 0xff).toByte()
            header[off + 1] = ((v shr 8) and 0xff).toByte()
            header[off + 2] = ((v shr 16) and 0xff).toByte()
            header[off + 3] = ((v shr 24) and 0xff).toByte()
        }
        fun putShort(off: Int, v: Int) {
            header[off] = (v and 0xff).toByte()
            header[off + 1] = ((v shr 8) and 0xff).toByte()
        }
        "RIFF".toByteArray().copyInto(header, 0)
        putInt(4, 36 + dataLen)
        "WAVE".toByteArray().copyInto(header, 8)
        "fmt ".toByteArray().copyInto(header, 12)
        putInt(16, 16)
        putShort(20, 1)
        putShort(22, 1)
        putInt(24, SAMPLE_RATE)
        putInt(28, SAMPLE_RATE * 2)
        putShort(32, 2)
        putShort(34, 16)
        "data".toByteArray().copyInto(header, 36)
        putInt(40, dataLen)
        fos.write(header)
        val bytes = ByteArray(dataLen)
        for (i in samples.indices) {
            bytes[i * 2] = (samples[i].toInt() and 0xff).toByte()
            bytes[i * 2 + 1] = ((samples[i].toInt() shr 8) and 0xff).toByte()
        }
        fos.write(bytes)
        fos.close()
    }
}
