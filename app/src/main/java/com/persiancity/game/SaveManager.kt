package com.persiancity.game

import android.content.Context
import org.json.JSONObject
import java.io.File

/**
 * مدیریت ذخیره و بارگذاری بازی (فرمت JSON در حافظه داخلی گوشی)
 * دنیای بازی این کلاس را از جاوا صدا می‌زند.
 */
object SaveManager {

    private const val FILE_NAME = "shahrshadi_save.json"

    fun hasSave(context: Context): Boolean {
        return File(context.filesDir, FILE_NAME).exists()
    }

    fun deleteSave(context: Context) {
        File(context.filesDir, FILE_NAME).delete()
    }

    @JvmStatic
    fun save(context: Context, data: JSONObject) {
        try {
            val tmp = File(context.filesDir, "$FILE_NAME.tmp")
            tmp.writeText(data.toString())
            val dst = File(context.filesDir, FILE_NAME)
            if (dst.exists()) dst.delete()
            tmp.renameTo(dst)
        } catch (_: Exception) {
        }
    }

    @JvmStatic
    fun load(context: Context): JSONObject? {
        return try {
            val f = File(context.filesDir, FILE_NAME)
            if (f.exists()) JSONObject(f.readText()) else null
        } catch (_: Exception) {
            null
        }
    }
}
