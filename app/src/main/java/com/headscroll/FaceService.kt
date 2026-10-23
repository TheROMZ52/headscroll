package com.headscroll

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.SystemClock
import android.util.Size
import androidx.camera.core.CameraSelector
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs

class FaceService : LifecycleService() {

    private lateinit var executor: ExecutorService
    private lateinit var detector: FaceDetector
    private lateinit var prefs: Prefs

    private var baseline: Float? = null
    private var smoothed: Float? = null
    private var armed = true
    private var lastTrigger = 0L

    override fun onCreate() {
        super.onCreate()
        running = true
        prefs = Prefs(this)
        executor = Executors.newSingleThreadExecutor()
        detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .build()
        )
        startAsForeground()
        startCamera()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_RECALIBRATE) {
            baseline = null
            smoothed = null
            armed = true
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        runCatching { ProcessCameraProvider.getInstance(this).get().unbindAll() }
        detector.close()
        executor.shutdown()
        super.onDestroy()
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "HeadScroll", NotificationManager.IMPORTANCE_LOW)
        )
        val pi = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE
        )
        val notif: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .setContentTitle("HeadScroll فعاله")
            .setContentText("با تکون دادن سر ریلز عوض میشه")
            .setContentIntent(pi)
            .setOngoing(true)
            .build()
        ServiceCompat.startForeground(this, 1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)
    }

    @Suppress("DEPRECATION")
    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            val provider = future.get()
            val analysis = ImageAnalysis.Builder()
                .setTargetResolution(Size(480, 360))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
            analysis.setAnalyzer(executor) { proxy -> analyze(proxy) }
            provider.unbindAll()
            provider.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, analysis)
        }, ContextCompat.getMainExecutor(this))
    }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun analyze(proxy: ImageProxy) {
        val media = proxy.image
        if (media == null) {
            proxy.close()
            return
        }
        val image = InputImage.fromMediaImage(media, proxy.imageInfo.rotationDegrees)
        detector.process(image)
            .addOnSuccessListener { faces ->
                val face = faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() }
                if (face != null) handle(face.headEulerAngleX)
            }
            .addOnCompleteListener { proxy.close() }
    }

    /** headEulerAngleX: مثبت = سر بالا، منفی = سر پایین */
    private fun handle(raw: Float) {
        val s = smoothed?.let { it + 0.5f * (raw - it) } ?: raw
        smoothed = s

        val b = baseline
        if (b == null) {
            baseline = s
            return
        }

        val delta = s - b
        val th = prefs.threshold.toFloat()
        val now = SystemClock.elapsedRealtime()

        if (!armed) {
            if (abs(delta) < th * 0.5f) {
                armed = true
            } else if (now - lastTrigger > 2500) {
                // سرت تو حالت جدید موند: اینو حالت عادی حساب کن
                baseline = s
                armed = true
            }
            return
        }

        if (abs(delta) < th * 0.5f) {
            baseline = b * 0.97f + s * 0.03f
            return
        }

        if (abs(delta) >= th && now - lastTrigger > prefs.cooldownMs) {
            lastTrigger = now
            armed = false
            ScrollAccessibilityService.instance?.swipe(next = delta > 0)
        }
    }

    companion object {
        const val CHANNEL = "headscroll"
        const val ACTION_RECALIBRATE = "com.headscroll.RECALIBRATE"
        @Volatile var running = false
    }
}
