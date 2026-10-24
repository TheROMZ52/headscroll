package com.headscroll

/** وضعیت زنده‌ی سرویس برای نمایش تو UI */
object LiveState {
    @Volatile var running = false
    @Volatile var paused = false
    @Volatile var cameraOn = false
    @Volatile var face = false
    @Volatile var dPitch = 0f
    @Volatile var dYaw = 0f
    @Volatile var smile = 0f
    @Volatile var hand = ""
    @Volatile var handReady = false
    @Volatile var handError = false
    @Volatile var calibrating = false
    @Volatile var calProgress = 0f
    @Volatile var wizardStep = 0
    @Volatile var wizardMsg = ""
    @Volatile var wizardProgress = 0f
    @Volatile var wizardResult = ""
    @Volatile var wizardResultAt = 0L
    @Volatile var lastGesture = ""
    @Volatile var lastGestureAt = 0L
    @Volatile var lastPerformed = false
}
