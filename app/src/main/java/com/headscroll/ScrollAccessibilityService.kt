package com.headscroll

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent

class ScrollAccessibilityService : AccessibilityService() {

    private val main = Handler(Looper.getMainLooper())

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == "com.android.systemui" || pkg.contains("inputmethod")) return
        currentPkg = pkg
    }

    override fun onInterrupt() {}

    override fun onUnbind(intent: android.content.Intent?): Boolean {
        if (instance === this) instance = null
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    /** next=true: ریلز بعدی (انگشت از پایین به بالا)، false: ریلز قبلی */
    fun swipe(next: Boolean) {
        if (Prefs(this).onlyInstagram && !currentPkg.startsWith("com.instagram")) return
        main.post {
            val dm = resources.displayMetrics
            val x = dm.widthPixels / 2f
            val top = dm.heightPixels * 0.25f
            val bottom = dm.heightPixels * 0.75f
            val path = Path().apply {
                if (next) {
                    moveTo(x, bottom); lineTo(x, top)
                } else {
                    moveTo(x, top); lineTo(x, bottom)
                }
            }
            val gesture = GestureDescription.Builder()
                .addStroke(GestureDescription.StrokeDescription(path, 0, 250))
                .build()
            dispatchGesture(gesture, null, null)
        }
    }

    companion object {
        @Volatile var instance: ScrollAccessibilityService? = null
        @Volatile var currentPkg: String = ""
    }
}
