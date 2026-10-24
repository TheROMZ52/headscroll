package com.headscroll

import android.content.Context
import java.time.LocalDate

class Prefs(context: Context) {
    private val sp = context.applicationContext.getSharedPreferences("headscroll", Context.MODE_PRIVATE)

    /** زاویه بالا/پایین (درجه) */
    var pitchTh: Int
        get() = sp.getInt("threshold", 15)
        set(v) = sp.edit().putInt("threshold", v).apply()

    /** زاویه چپ/راست (درجه) */
    var yawTh: Int
        get() = sp.getInt("yawTh", 20)
        set(v) = sp.edit().putInt("yawTh", v).apply()

    var holdMs: Int
        get() = sp.getInt("holdMs", 120)
        set(v) = sp.edit().putInt("holdMs", v).apply()

    var cooldownMs: Int
        get() = sp.getInt("cooldown", 1200)
        set(v) = sp.edit().putInt("cooldown", v).apply()

    var swipeMs: Int
        get() = sp.getInt("swipeMs", 250)
        set(v) = sp.edit().putInt("swipeMs", v).apply()

    var vibrate: Boolean
        get() = sp.getBoolean("vibrate", true)
        set(v) = sp.edit().putBoolean("vibrate", v).apply()

    var smartCamera: Boolean
        get() = sp.getBoolean("smartCamera", true)
        set(v) = sp.edit().putBoolean("smartCamera", v).apply()

    var autoCal: Boolean
        get() = sp.getBoolean("autoCal", true)
        set(v) = sp.edit().putBoolean("autoCal", v).apply()

    var allApps: Boolean
        get() = sp.getBoolean("allApps", false)
        set(v) = sp.edit().putBoolean("allApps", v).apply()

    var invertV: Boolean
        get() = sp.getBoolean("invertV", false)
        set(v) = sp.edit().putBoolean("invertV", v).apply()

    var invertH: Boolean
        get() = sp.getBoolean("invertH", false)
        set(v) = sp.edit().putBoolean("invertH", v).apply()

    var accConsent: Boolean
        get() = sp.getBoolean("accConsent", false)
        set(v) = sp.edit().putBoolean("accConsent", v).apply()

    var targets: Set<String>
        get() = sp.getStringSet("targets", setOf("instagram")) ?: setOf("instagram")
        set(v) = sp.edit().putStringSet("targets", HashSet(v)).apply()

    fun targetPackages(): List<String> =
        Targets.all.filter { targets.contains(it.key) }.flatMap { it.packages }

    fun action(t: Trigger): Action =
        runCatching { Action.valueOf(sp.getString("act_" + t.key, t.default.name) ?: t.default.name) }
            .getOrDefault(t.default)

    fun setAction(t: Trigger, a: Action) {
        sp.edit().putString("act_" + t.key, a.name).apply()
    }

    fun applyPreset(i: Int) {
        val (p, y, h, c) = when (i) {
            0 -> listOf(20, 28, 200, 1500)
            2 -> listOf(10, 14, 60, 800)
            else -> listOf(15, 20, 120, 1200)
        }
        pitchTh = p; yawTh = y; holdMs = h; cooldownMs = c
    }

    fun countToday(): Int =
        if (sp.getString("cDate", "") == LocalDate.now().toString()) sp.getInt("cToday", 0) else 0

    fun countTotal(): Int = sp.getInt("cTotal", 0)

    fun incCount() {
        val today = LocalDate.now().toString()
        val e = sp.edit()
        if (sp.getString("cDate", "") != today) {
            e.putString("cDate", today)
            e.putInt("cToday", 1)
        } else {
            e.putInt("cToday", sp.getInt("cToday", 0) + 1)
        }
        e.putInt("cTotal", sp.getInt("cTotal", 0) + 1)
        e.apply()
    }

    fun resetDefaults() {
        val e = sp.edit()
        listOf(
            "threshold", "yawTh", "holdMs", "cooldown", "swipeMs", "vibrate", "smartCamera",
            "autoCal", "allApps", "invertV", "invertH", "targets"
        ).forEach { e.remove(it) }
        Trigger.values().forEach { e.remove("act_" + it.key) }
        e.apply()
    }
}
