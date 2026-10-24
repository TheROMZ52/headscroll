package com.headscroll

import android.content.Context
import android.graphics.Bitmap
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.handlandmarker.HandLandmarker
import kotlin.math.sqrt

/** تشخیص ژست دست با MediaPipe (مدل hand_landmarker.task داخل assets) */
class HandDetector(ctx: Context) {
    private var landmarker: HandLandmarker? = null
    val available: Boolean get() = landmarker != null

    init {
        try {
            val base = BaseOptions.builder().setModelAssetPath("hand_landmarker.task").build()
            val opts = HandLandmarker.HandLandmarkerOptions.builder()
                .setBaseOptions(base)
                .setNumHands(1)
                .setRunningMode(RunningMode.IMAGE)
                .setMinHandDetectionConfidence(0.55f)
                .setMinHandPresenceConfidence(0.5f)
                .build()
            landmarker = HandLandmarker.createFromOptions(ctx.applicationContext, opts)
        } catch (e: Throwable) {
            landmarker = null
        }
    }

    fun detect(bmp: Bitmap): Trigger? {
        val hl = landmarker ?: return null
        val res = hl.detect(BitmapImageBuilder(bmp).build())
        val all = res.landmarks()
        if (all.isEmpty()) return null
        val l = all[0]
        val w = bmp.width.toFloat()
        val h = bmp.height.toFloat()
        fun d(a: Int, b: Int): Float {
            val dx = (l[a].x() - l[b].x()) * w
            val dy = (l[a].y() - l[b].y()) * h
            return sqrt(dx * dx + dy * dy)
        }
        val palm = d(0, 9).coerceAtLeast(1f)
        fun ext(tip: Int, pip: Int) = d(tip, 0) > d(pip, 0) * 1.15f
        val index = ext(8, 6)
        val middle = ext(12, 10)
        val ring = ext(16, 14)
        val pinky = ext(20, 18)
        val pinch = d(4, 8) / palm
        val others = listOf(middle, ring, pinky).count { it }
        return when {
            pinch < 0.40f && others >= 2 -> Trigger.HAND_OK
            pinch < 0.40f -> Trigger.HAND_PINCH
            index && middle && !ring && !pinky -> Trigger.HAND_VICTORY
            index && middle && ring && pinky && pinch > 0.8f -> Trigger.HAND_PALM
            else -> null
        }
    }

    fun close() {
        runCatching { landmarker?.close() }
        landmarker = null
    }
}
