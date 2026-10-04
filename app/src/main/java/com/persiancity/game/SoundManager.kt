package com.persiancity.game

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.sin

/**
 * موتور صدای شهر شادی — همه افکت‌ها با AudioTrack مستقیم پخش می‌شوند
 * (بدون فایل و SoundPool تا روی همه گوشی‌ها قطعاً صدا داشته باشیم)
 * + موسیقی پس‌زمینه شاد + صدای زنده موتور و بال‌زنی هلیکوپتر.
 */
object SoundManager {

    private const val SR = 22050

    private var prefs: SharedPreferences? = null

    @get:JvmStatic
    var isMuted: Boolean = false
        private set

    private val cache = HashMap<String, ShortArray>()
    private val activeTracks = ArrayList<AudioTrack>()

    private var musicTrack: AudioTrack? = null
    private var musicThread: Thread? = null
    @Volatile
    private var musicRunning = false

    private var engineTrack: AudioTrack? = null
    private var engineThread: Thread? = null
    @Volatile
    private var engineRunning = false
    @Volatile
    private var engineIntensity = 0f
    @Volatile
    private var engineChop = false

    @JvmStatic
    fun init(context: Context) {
        if (cache.isNotEmpty()) return
        prefs = context.getSharedPreferences("shahrshadi_prefs", Context.MODE_PRIVATE)
        isMuted = prefs?.getBoolean("muted", false) ?: false

        cache["click"] = tone(880.0, 0.06, 0.5, square = true)
        cache["coin"] = concat(tone(988.0, 0.09, 0.5), tone(1319.0, 0.16, 0.5))
        cache["buy"] = concat(tone(660.0, 0.07, 0.5), tone(990.0, 0.07, 0.5), tone(1320.0, 0.14, 0.5))
        cache["success"] = concat(
            tone(523.0, 0.10, 0.5), tone(659.0, 0.10, 0.5),
            tone(784.0, 0.10, 0.5), tone(1047.0, 0.22, 0.5)
        )
        cache["door"] = slide(220.0, 110.0, 0.14, 0.5)
        cache["eat"] = concat(tone(392.0, 0.08, 0.4), tone(523.0, 0.10, 0.4))
        cache["horn"] = mix(
            tone(350.0, 0.35, 0.4, square = true),
            tone(440.0, 0.35, 0.4, square = true)
        )
        cache["train"] = concat(tone(196.0, 0.30, 0.5), tone(262.0, 0.45, 0.5))
        cache["mission"] = concat(
            tone(784.0, 0.09, 0.5), tone(988.0, 0.09, 0.5),
            tone(1175.0, 0.09, 0.5), tone(1568.0, 0.25, 0.5)
        )
        cache["crash"] = crashSound()
        cache["whoosh"] = slide(600.0, 150.0, 0.3, 0.3)
        cache["splash"] = slide(900.0, 180.0, 0.28, 0.45)
        cache["fish"] = concat(tone(1046.0, 0.07, 0.5), tone(784.0, 0.14, 0.5))

        // 🚓 آژیر پلیس
        cache["siren"] = concat(slide(650.0, 900.0, 0.40, 0.26), slide(900.0, 650.0, 0.40, 0.26))
        // 🐴 شیهه اسب / گورخر
        cache["neigh"] = concat(slide(700.0, 240.0, 0.35, 0.45), tone(240.0, 0.15, 0.28))
        // 🦁 غرش شیر
        cache["lion"] = growl(80.0, 0.9)
        // 🐯 غرش ببر
        cache["tiger"] = growl(115.0, 0.8)
        // 🐻 غرش خرس
        cache["bear"] = growl(58.0, 1.0)
        // 🐑 فیل (شیپور)
        cache["elephant"] = concat(slide(430.0, 160.0, 0.55, 0.45), slide(160.0, 300.0, 0.25, 0.32))
        // 🐒 میمون
        cache["monkey"] = concat(
            slide(850.0, 1500.0, 0.10, 0.38), slide(950.0, 1600.0, 0.10, 0.38),
            slide(800.0, 1400.0, 0.12, 0.38)
        )
        // 🐦 پرنده / پنگوئن / پاندا
        cache["birds"] = concat(
            slide(1900.0, 2500.0, 0.07, 0.30), slide(2100.0, 2700.0, 0.07, 0.30),
            slide(1800.0, 2400.0, 0.08, 0.30)
        )
        // 🦒 زرافه
        cache["giraffe"] = slide(280.0, 360.0, 0.6, 0.20)
        // 🐪 شتر
        cache["camel"] = concat(slide(320.0, 180.0, 0.4, 0.42), slide(200.0, 260.0, 0.2, 0.28))
        // 🐄 صدای گاو روستا
        cache["cow"] = concat(slide(200.0, 120.0, 0.45, 0.5), slide(120.0, 150.0, 0.3, 0.4))
        // 🐑 صدای گوسفند (بع بع)
        cache["sheep"] = concat(
            slide(520.0, 420.0, 0.16, 0.45), slide(430.0, 500.0, 0.12, 0.40),
            slide(500.0, 380.0, 0.20, 0.38)
        )
        // ✈ موتور جت (برخاست/فرود)
        cache["jet"] = slide(110.0, 430.0, 1.3, 0.20)
    }

    // ---------------- پخش افکت ----------------

    @JvmStatic
    fun play(name: String) {
        if (isMuted) return
        val samples = cache[name] ?: return
        playSamples(samples)
    }

    private fun playSamples(samples: ShortArray) {
        try {
            synchronized(activeTracks) {
                activeTracks.removeAll { t ->
                    try { t.playState != AudioTrack.PLAYSTATE_PLAYING } catch (_: Exception) { true }
                }
                if (activeTracks.size >= 6) return
            }
            val durationMs = samples.size * 1000L / SR
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SR)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()
            track.write(samples, 0, samples.size)
            track.play()
            synchronized(activeTracks) { activeTracks.add(track) }
            Thread {
                try {
                    Thread.sleep(durationMs + 150)
                    track.stop()
                    track.release()
                } catch (_: Exception) {
                }
                synchronized(activeTracks) { activeTracks.remove(track) }
            }.start()
        } catch (_: Exception) {
        }
    }

    @JvmStatic
    fun setMuted(context: Context, muted: Boolean) {
        isMuted = muted
        prefs?.edit()?.putBoolean("muted", muted)?.apply()
        if (muted) {
            stopEngine()
            stopMusic()
        } else {
            startMusic()
        }
    }

    // ---------------- صدای زنده موتور / هلیکوپتر ----------------

    @JvmStatic
    fun startEngine() {
        if (isMuted || engineRunning) return
        try {
            val minBuf = AudioTrack.getMinBufferSize(
                SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
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
                        .setSampleRate(SR)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            engineTrack?.play()
            engineRunning = true
            engineThread = Thread {
                var phase = 0.0
                var chopPhase = 0.0
                val buf = ShortArray(1024)
                var smoothFreq = 60.0
                while (engineRunning) {
                    val freq = 55.0 + engineIntensity * 130.0
                    smoothFreq = smoothFreq * 0.85 + freq * 0.15
                    var vol = 0.16 + engineIntensity * 0.14
                    for (i in buf.indices) {
                        phase += 2 * PI * smoothFreq / SR
                        if (phase > 2 * PI) phase -= 2 * PI
                        val saw = 2.0 * (phase / (2 * PI)) - 1.0
                        var soft = 0.6 * sin(phase) + 0.4 * saw
                        if (engineChop) {
                            chopPhase += 2 * PI * 14.0 / SR
                            if (chopPhase > 2 * PI) chopPhase -= 2 * PI
                            soft *= (0.45 + 0.55 * abs(sin(chopPhase)))
                            vol = 0.14 + engineIntensity * 0.10
                        }
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
    fun setEngineChop(chop: Boolean) {
        engineChop = chop
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

    // ---------------- موسیقی پس‌زمینه شاد ----------------

    @JvmStatic
    fun startMusic() {
        if (isMuted || musicRunning) return
        try {
            val loop = musicLoop()
            val minBuf = AudioTrack.getMinBufferSize(
                SR, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT
            )
            musicTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_GAME)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SR)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBuf * 4)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            musicTrack?.play()
            musicRunning = true
            musicThread = Thread {
                while (musicRunning) {
                    try {
                        musicTrack?.write(loop, 0, loop.size)
                    } catch (_: Exception) {
                        break
                    }
                }
            }
            musicThread?.start()
        } catch (_: Exception) {
        }
    }

    @JvmStatic
    fun stopMusic() {
        musicRunning = false
        try {
            musicThread?.join(300)
            musicTrack?.stop()
            musicTrack?.release()
        } catch (_: Exception) {
        }
        musicTrack = null
        musicThread = null
    }

    /**
     * حلقه موسیقی شاد ۸ میزانی — پنتاتونیک ماژور با بیس و های‌هت ملایم
     */
    private fun musicLoop(): ShortArray {
        val bpm = 104.0
        val eighth = 60.0 / bpm / 2.0
        val steps = 64
        val total = (steps * eighth * SR).toInt()
        val out = ShortArray(total)

        val scale = intArrayOf(0, 2, 4, 7, 9)
        val melody = intArrayOf(
            0, -1, 2, 2, 4, -1, 2, -1,
            0, -1, 2, 4, 7, -1, 4, 2,
            4, -1, 5, 5, 7, -1, 5, 4,
            2, 4, 2, 0, -1, -1, 0, -1,
            7, -1, 9, 9, 7, -1, 5, 4,
            5, -1, 7, 5, 4, -1, 2, 0,
            2, -1, 0, 2, 4, 2, 0, -1,
            0, 2, 4, 5, 7, 9, 7, -1
        )
        val bassFreqs = doubleArrayOf(
            130.8, 130.8, 196.0, 196.0,
            220.0, 220.0, 174.6, 174.6
        )

        fun noteFreq(step: Int): Double {
            val deg = melody[step % melody.size]
            if (deg < 0) return 0.0
            val oct = deg / scale.size
            val semi = scale[deg % scale.size]
            return 261.63 * Math.pow(2.0, (semi + 12 * oct) / 12.0)
        }

        var idx = 0
        for (step in 0 until steps) {
            val n = (eighth * SR).toInt()

            val f = noteFreq(step)
            if (f > 0) {
                for (i in 0 until n) {
                    val t = i.toDouble() / SR
                    val env = if (i < n * 0.1) i / (n * 0.1) else 1.0 - (i - n * 0.1) / n * 0.55
                    val v = 0.30 * env * (sin(2 * PI * f * t) + 0.28 * sin(4 * PI * f * t))
                    if (idx + i < total) out[idx + i] = (out[idx + i] + v * Short.MAX_VALUE)
                        .toInt().coerceIn(-32767, 32767).toShort()
                }
            }

            if (step % 8 == 0 || step % 8 == 4) {
                val bf = bassFreqs[(step / 8) % bassFreqs.size]
                for (i in 0 until n) {
                    val t = i.toDouble() / SR
                    val env = 1.0 - i.toDouble() / n * 0.6
                    val v = 0.22 * env * sin(2 * PI * bf * t)
                    if (idx + i < total) out[idx + i] = (out[idx + i] + v * Short.MAX_VALUE)
                        .toInt().coerceIn(-32767, 32767).toShort()
                }
            }

            if (step % 2 == 0) {
                for (i in 0 until n / 6) {
                    val env = 1.0 - i.toDouble() / (n / 6)
                    val v = 0.05 * env * (if ((i * 7919) % 2 == 0) 1.0 else -1.0)
                    if (idx + i < total) out[idx + i] = (out[idx + i] + v * Short.MAX_VALUE)
                        .toInt().coerceIn(-32767, 32767).toShort()
                }
            }

            idx += n
        }

        val fade = 600
        for (i in 0 until fade) {
            val a = i.toDouble() / fade
            val head = out[i].toDouble()
            val tail = out[total - fade + i].toDouble()
            out[i] = (head * a + tail * (1 - a)).toInt().toShort()
        }
        return out
    }

    // ---------------- ساخت موج صوتی ----------------

    private fun tone(freq: Double, dur: Double, vol: Double, square: Boolean = false): ShortArray {
        val n = (SR * dur).toInt()
        val out = ShortArray(n)
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            val env = 1.0 - (i.toDouble() / n) * 0.7
            var v = sin(2 * PI * freq * t)
            if (square) v = if (v >= 0) 0.7 else -0.7
            out[i] = (v * env * vol * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun slide(f1: Double, f2: Double, dur: Double, vol: Double): ShortArray {
        val n = (SR * dur).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val p = i.toDouble() / n
            val f = f1 + (f2 - f1) * p
            phase += 2 * PI * f / SR
            out[i] = (sin(phase) * (1 - p) * vol * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    private fun crashSound(): ShortArray {
        val n = (SR * 0.45).toInt()
        val out = ShortArray(n)
        var last = 0.0
        for (i in 0 until n) {
            val p = i.toDouble() / n
            val env = (1.0 - p) * (1.0 - p)
            val noise = (if ((i * 1103515245 + 12345) % 2 == 0) 1.0 else -1.0) * 0.55
            val thump = sin(2 * PI * (70.0 - 40.0 * p) * i / SR) * 0.7
            last = last * 0.7 + (noise * 0.5 + thump) * 0.3
            out[i] = (last * env * 0.85 * Short.MAX_VALUE).toInt().toShort()
        }
        return out
    }

    /** غرش حیوانات — اره‌ای بم با لرزش و نویز */
    private fun growl(freq: Double, dur: Double): ShortArray {
        val n = (SR * dur).toInt()
        val out = ShortArray(n)
        var phase = 0.0
        for (i in 0 until n) {
            val t = i.toDouble() / SR
            phase += 2 * PI * (freq + 12.0 * sin(2 * PI * 9.0 * t)) / SR
            val cyc = phase / (2 * PI)
            val saw = 2.0 * (cyc - kotlin.math.floor(cyc)) - 1.0
            val noise = (if ((i * 2654435761L + 1013904223L) % 2 == 0L) 1.0 else -1.0) * 0.16
            val env = min(1.0, i / (SR * 0.08)) * (1.0 - i.toDouble() / n * 0.35)
            out[i] = ((saw * 0.55 + noise) * env * 0.5 * Short.MAX_VALUE).toInt().toShort()
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
}
