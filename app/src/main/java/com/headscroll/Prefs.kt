package com.headscroll

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("headscroll", Context.MODE_PRIVATE)

    /** زاویه لازم (درجه) برای تشخیص حرکت سر */
    var threshold: Int
        get() = sp.getInt("threshold", 15)
        set(v) = sp.edit().putInt("threshold", v).apply()

    /** فاصله بین دو سوایپ (میلی‌ثانیه) */
    var cooldownMs: Int
        get() = sp.getInt("cooldown", 1200)
        set(v) = sp.edit().putInt("cooldown", v).apply()

    var onlyInstagram: Boolean
        get() = sp.getBoolean("onlyInstagram", true)
        set(v) = sp.edit().putBoolean("onlyInstagram", v).apply()
}
