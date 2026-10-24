package com.headscroll

enum class Action(val label: String) {
    NONE("هیچ‌کاری"),
    NEXT("ریلز بعدی ⬆️"),
    PREV("ریلز قبلی ⬇️"),
    LIKE("لایک ❤️ (دابل‌تپ)"),
    TAP("پخش / توقف ⏯ (تپ)"),
    BACK("برگشت ◀️"),
    NEXT_H("بعدی ➡️ (سوایپ افقی)"),
    PREV_H("قبلی ⬅️ (سوایپ افقی)")
}

enum class Trigger(
    val key: String,
    val label: String,
    val emoji: String,
    val default: Action,
    val hand: Boolean = false
) {
    UP("up", "سر بالا", "⬆️", Action.NEXT),
    DOWN("down", "سر پایین", "⬇️", Action.PREV),
    LEFT("left", "سر به چپ", "⬅️", Action.PREV_H),
    RIGHT("right", "سر به راست", "➡️", Action.NEXT_H),
    SMILE("smile", "لبخند", "😄", Action.NONE),
    HAND_OK("ok", "علامت OK", "👌", Action.LIKE, true),
    HAND_PINCH("pinch", "پینچ (استپ)", "🤏", Action.TAP, true),
    HAND_PALM("palm", "کف دست", "✋", Action.NONE, true),
    HAND_VICTORY("victory", "ویکتوری", "✌️", Action.NONE, true)
}

data class AppTarget(val key: String, val label: String, val packages: List<String>)

object Targets {
    val all = listOf(
        AppTarget("instagram", "اینستاگرام", listOf("com.instagram.android")),
        AppTarget("tiktok", "تیک‌تاک", listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")),
        AppTarget("youtube", "یوتیوب (Shorts)", listOf("com.google.android.youtube")),
        AppTarget("facebook", "فیسبوک", listOf("com.facebook.katana")),
        AppTarget("snapchat", "اسنپ‌چت", listOf("com.snapchat.android"))
    )
}
