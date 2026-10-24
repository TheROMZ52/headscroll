package com.headscroll

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.graphics.Bitmap
import android.graphics.Matrix
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
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
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetector
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FaceService : LifecycleService() {

    private lateinit var executor: ExecutorService
    private lateinit var detector: FaceDetector
    private lateinit var prefs: Prefs
    private lateinit var analysis: ImageAnalysis
    private val main = Handler(Looper.getMainLooper())

    @Volatile private var hands: HandDetector? = null
    @Volatile private var handNow: Trigger? = null
    @Volatile private var handAt = 0L
    private var lastHand = 0L
    private var lastFrame = 0L

    private var cameraWanted = false
    private var screenOn = true

    // کالیبراسیون و وضعیت حرکت سر
    private var baseP: Float? = null
    private var baseY: Float? = null
    private var smP: Float? = null
    private var smY: Float? = null
    private var calStart = 0L
    private var calSumP = 0f
    private var calSumY = 0f
    private var calN = 0
    private var lastFaceAt = 0L

    private var armed = true
    private var lastTrigger = 0L
    private var pending: Trigger? = null
    private var pendingSince = 0L

    // ویزارد کالیبراسیون هوشمند: 0 خاموش، 1 صاف، 2 بالا، 3 پایین، 4 چپ، 5 راست
    private var wizard = 0
    private var wizStart = 0L
    private var wizSumP = 0f
    private var wizSumY = 0f
    private var wizN = 0
    private val wizPeak = FloatArray(6)

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, i: Intent?) {
            screenOn = i?.action != Intent.ACTION_SCREEN_OFF
            applyCamera()
        }
    }

    private val ticker = object : Runnable {
        override fun run() {
            if (wizard > 0 && SystemClock.elapsedRealtime() - wizStart > 40000L) cancelWizard()
            applyCamera()
            main.postDelayed(this, 1000L)
        }
    }

    override fun onCreate() {
        super.onCreate()
        LiveState.running = true
        LiveState.paused = false
        prefs = Prefs(this)
        executor = Executors.newSingleThreadExecutor()
        detector = FaceDetection.getClient(
            FaceDetectorOptions.Builder()
                .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
                .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
                .build()
        )
        analysis = buildAnalysis()
        screenOn = getSystemService(PowerManager::class.java).isInteractive
        lastFaceAt = SystemClock.elapsedRealtime()

        ContextCompat.registerReceiver(
            this, screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ScrollAccessibilityService.onPackageChanged = {
            main.post {
                if (prefs.autoCal && wizard == 0) resetBaseline()
                applyCamera()
            }
        }

        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL, "HeadScroll", NotificationManager.IMPORTANCE_LOW)
        )
        ServiceCompat.startForeground(this, 1, buildNotification(), ServiceInfo.FOREGROUND_SERVICE_TYPE_CAMERA)

        // مدل دست رو تو ترد جدا لود کن
        executor.execute {
            val h = HandDetector(this)
            hands = h
            LiveState.handReady = h.available
            LiveState.handError = !h.available
        }
        main.post(ticker)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        when (intent?.action) {
            ACTION_STOP -> {
                stopSelf()
                return START_NOT_STICKY
            }
            ACTION_RECALIBRATE -> resetBaseline()
            ACTION_WIZARD -> startWizard()
            ACTION_TOGGLE_PAUSE -> {
                LiveState.paused = !LiveState.paused
                resetBaseline()
                updateNotification()
            }
        }
        applyCamera()
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        LiveState.running = false
        LiveState.cameraOn = false
        LiveState.face = false
        LiveState.wizardStep = 0
        LiveState.calibrating = false
        LiveState.handReady = false
        ScrollAccessibilityService.onPackageChanged = null
        main.removeCallbacks(ticker)
        runCatching { unregisterReceiver(screenReceiver) }
        runCatching { ProcessCameraProvider.getInstance(this).get().unbindAll() }
        detector.close()
        hands?.close()
        executor.shutdown()
        super.onDestroy()
    }

    // ---------- نوتیفیکیشن ----------

    private fun buildNotification(): Notification {
        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE
        )
        fun act(a: String, code: Int) = PendingIntent.getService(
            this, code, Intent(this, FaceService::class.java).setAction(a),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val paused = LiveState.paused
        return NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_tile)
            .setContentTitle(if (paused) "HeadScroll مکث شده" else "HeadScroll فعاله")
            .setContentText(if (paused) "برای ادامه «ادامه» رو بزن" else "با تکون دادن سر یا ژست دست کنترل کن")
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .addAction(0, "توقف", act(ACTION_STOP, 1))
            .addAction(0, if (paused) "ادامه" else "مکث", act(ACTION_TOGGLE_PAUSE, 2))
            .build()
    }

    private fun updateNotification() {
        getSystemService(NotificationManager::class.java).notify(1, buildNotification())
    }

    // ---------- دوربین ----------

    @Suppress("DEPRECATION")
    private fun buildAnalysis(): ImageAnalysis {
        val a = ImageAnalysis.Builder()
            .setTargetResolution(Size(480, 360))
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
        a.setAnalyzer(executor) { proxy -> analyze(proxy) }
        return a
    }

    private fun wantCamera(): Boolean {
        if (LiveState.paused || !screenOn) return false
        if (wizard > 0) return true
        if (prefs.allApps || !prefs.smartCamera) return true
        // اگه Accessibility وصل نیست دوربین رو روشن نگه دار تا تست زنده کار کنه
        if (ScrollAccessibilityService.instance == null) return true
        val pk = ScrollAccessibilityService.currentPkg
        if (pk == packageName) return true
        return prefs.targetPackages().contains(pk)
    }

    private fun applyCamera() {
        val want = wantCamera()
        if (want == cameraWanted) return
        cameraWanted = want
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            runCatching {
                val p = future.get()
                if (cameraWanted) {
                    p.unbindAll()
                    p.bindToLifecycle(this, CameraSelector.DEFAULT_FRONT_CAMERA, analysis)
                } else {
                    p.unbindAll()
                }
            }
            LiveState.cameraOn = cameraWanted
        }, ContextCompat.getMainExecutor(this))
        if (!want) {
            LiveState.face = false
            LiveState.hand = ""
            if (wizard == 0) resetBaseline()
        }
    }

    // ---------- تحلیل فریم ----------

    private fun handEnabled(): Boolean =
        Trigger.values().any { it.hand && prefs.action(it) != Action.NONE }

    @androidx.annotation.OptIn(ExperimentalGetImage::class)
    private fun analyze(proxy: ImageProxy) {
        val now = SystemClock.elapsedRealtime()
        if (now - lastFrame < 66L) {
            proxy.close()
            return
        }
        lastFrame = now
        val media = proxy.image
        if (media == null) {
            proxy.close()
            return
        }
        val deg = proxy.imageInfo.rotationDegrees

        val h = hands
        if (h != null && h.available && wizard == 0 && handEnabled()) {
            if (now - lastHand >= 110L) {
                lastHand = now
                runCatching {
                    val bmp = proxy.toBitmap()
                    val rot = if (deg != 0) {
                        Bitmap.createBitmap(
                            bmp, 0, 0, bmp.width, bmp.height,
                            Matrix().apply { postRotate(deg.toFloat()) }, true
                        )
                    } else bmp
                    handNow = h.detect(rot)
                    handAt = SystemClock.elapsedRealtime()
                }
            }
        } else {
            handNow = null
        }

        val image = InputImage.fromMediaImage(media, deg)
        detector.process(image)
            .addOnSuccessListener { faces ->
                onFrame(faces.maxByOrNull { it.boundingBox.width() * it.boundingBox.height() })
            }
            .addOnFailureListener { onFrame(null) }
            .addOnCompleteListener { proxy.close() }
    }

    private fun resetBaseline() {
        baseP = null
        baseY = null
        smP = null
        smY = null
        calStart = 0L
        calN = 0
        calSumP = 0f
        calSumY = 0f
        armed = true
        pending = null
        LiveState.calibrating = false
    }

    private fun calibrateStep(now: Long, sp: Float, sy: Float) {
        if (calStart == 0L) {
            calStart = now
            calN = 0
            calSumP = 0f
            calSumY = 0f
        }
        val t = now - calStart
        LiveState.calibrating = true
        LiveState.calProgress = min(1f, t / 1200f)
        if (t > 250L) {
            calSumP += sp
            calSumY += sy
            calN++
        }
        if (t >= 1200L && calN >= 4) {
            baseP = calSumP / calN
            baseY = calSumY / calN
            LiveState.calibrating = false
            armed = true
            pending = null
        }
    }

    /** headEulerAngleX: مثبت = سر بالا؛ headEulerAngleY: مثبت = سمت راست تصویر دوربین (چپ کاربر) */
    private fun onFrame(face: Face?) {
        val now = SystemClock.elapsedRealtime()
        val hand = if (now - handAt < 350L) handNow else null
        LiveState.hand = hand?.emoji ?: ""

        var mag = 0f
        var smile = 0f
        var headCand: Trigger? = null
        var headReady = false

        if (face == null) {
            LiveState.face = false
        } else {
            LiveState.face = true
            val gap = now - lastFaceAt
            lastFaceAt = now
            if (gap > 2000L && prefs.autoCal && wizard == 0 && baseP != null) resetBaseline()

            val rx = face.headEulerAngleX
            val ry = face.headEulerAngleY
            val sp = smP?.let { it + 0.5f * (rx - it) } ?: rx
            val sy = smY?.let { it + 0.5f * (ry - it) } ?: ry
            smP = sp
            smY = sy
            smile = face.smilingProbability ?: 0f
            LiveState.smile = smile

            if (wizard > 0) {
                wizardTick(now, sp, sy)
                return
            }

            val bp = baseP
            val by = baseY
            if (bp == null || by == null) {
                calibrateStep(now, sp, sy)
            } else {
                headReady = true
                val sv = if (prefs.invertV) -1f else 1f
                val sh = if (prefs.invertH) 1f else -1f
                val dp = (sp - bp) * sv
                val dy = (sy - by) * sh
                LiveState.dPitch = dp
                LiveState.dYaw = dy
                val np = dp / prefs.pitchTh
                val ny = dy / prefs.yawTh
                mag = max(abs(np), abs(ny))
                headCand = when {
                    mag >= 1f -> if (abs(np) >= abs(ny)) {
                        if (np > 0) Trigger.UP else Trigger.DOWN
                    } else {
                        if (ny > 0) Trigger.RIGHT else Trigger.LEFT
                    }
                    smile >= 0.85f -> Trigger.SMILE
                    else -> null
                }
                if (mag < 0.5f && armed && hand == null) {
                    baseP = bp * 0.97f + sp * 0.03f
                    baseY = by * 0.97f + sy * 0.03f
                }
            }
        }
        if (wizard > 0) return

        val cand: Trigger? = hand ?: headCand
        val calm = hand == null && mag < 0.5f && smile < 0.5f

        if (!armed) {
            if (calm) {
                armed = true
            } else if (headReady && hand == null && smile < 0.5f && now - lastTrigger > 2500L) {
                baseP = smP
                baseY = smY
                armed = true
            }
            pending = null
            return
        }

        if (cand == null || prefs.action(cand) == Action.NONE) {
            pending = null
            return
        }
        if (cand != pending) {
            pending = cand
            pendingSince = now
            return
        }
        val hold = if (cand.hand) max(prefs.holdMs, 280) else prefs.holdMs
        if (now - pendingSince < hold || now - lastTrigger < prefs.cooldownMs) return
        fire(cand, now)
    }

    private fun fire(t: Trigger, now: Long) {
        lastTrigger = now
        armed = false
        pending = null
        val a = prefs.action(t)
        val ok = ScrollAccessibilityService.instance?.perform(a) ?: false
        LiveState.lastGesture = "${t.emoji} ${t.label}  ←  ${a.label}"
        LiveState.lastGestureAt = now
        LiveState.lastPerformed = ok
        if (ok) prefs.incCount()
        if (prefs.vibrate && (ok || ScrollAccessibilityService.currentPkg == packageName)) vibrate()
    }

    private fun vibrate() {
        val v = getSystemService(Vibrator::class.java) ?: return
        v.vibrate(VibrationEffect.createOneShot(30, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    // ---------- کالیبراسیون هوشمند (ویزارد) ----------

    private fun startWizard() {
        resetBaseline()
        wizard = 1
        wizStart = SystemClock.elapsedRealtime()
        wizSumP = 0f
        wizSumY = 0f
        wizN = 0
        wizPeak.fill(0f)
        handNow = null
        LiveState.wizardStep = 1
        LiveState.wizardProgress = 0f
        LiveState.wizardMsg = "😐 سرتو صاف نگه دار"
        LiveState.wizardResult = ""
        applyCamera()
    }

    private fun cancelWizard() {
        wizard = 0
        LiveState.wizardStep = 0
        LiveState.wizardMsg = ""
        resetBaseline()
    }

    private fun wizardNext(step: Int, now: Long) {
        wizard = step
        wizStart = now
        LiveState.wizardStep = step
        LiveState.wizardProgress = 0f
        LiveState.wizardMsg = when (step) {
            2 -> "⬆️ سرتو تا جایی که راحتی ببر بالا"
            3 -> "⬇️ حالا ببر پایین"
            4 -> "⬅️ حالا بچرخون چپ"
            else -> "➡️ حالا بچرخون راست"
        }
    }

    private fun wizardTick(now: Long, sp: Float, sy: Float) {
        val t = now - wizStart
        if (wizard == 1) {
            LiveState.wizardProgress = min(1f, t / 2200f)
            if (t > 700L) {
                wizSumP += sp
                wizSumY += sy
                wizN++
            }
            if (t >= 2200L && wizN >= 3) {
                baseP = wizSumP / wizN
                baseY = wizSumY / wizN
                wizardNext(2, now)
            }
            return
        }
        val bp = baseP ?: return
        val by = baseY ?: return
        val sv = if (prefs.invertV) -1f else 1f
        val sh = if (prefs.invertH) 1f else -1f
        val dp = (sp - bp) * sv
        val dy = (sy - by) * sh
        LiveState.dPitch = dp
        LiveState.dYaw = dy
        val v = when (wizard) {
            2 -> dp
            3 -> -dp
            4 -> -dy
            else -> dy
        }
        if (t >= 600L && v > wizPeak[wizard]) wizPeak[wizard] = v
        LiveState.wizardProgress = min(1f, t / 3500f)
        if (t >= 3500L) {
            if (wizard < 5) wizardNext(wizard + 1, now) else finishWizard(now)
        }
    }

    private fun finishWizard(now: Long) {
        val p = min(wizPeak[2], wizPeak[3])
        val y = min(wizPeak[4], wizPeak[5])
        val parts = mutableListOf<String>()
        if (p >= 6f) {
            prefs.pitchTh = (p * 0.6f).toInt().coerceIn(8, 30)
            parts.add("بالا/پایین ${prefs.pitchTh}°")
        }
        if (y >= 6f) {
            prefs.yawTh = (y * 0.6f).toInt().coerceIn(10, 40)
            parts.add("چپ/راست ${prefs.yawTh}°")
        }
        LiveState.wizardResult = if (parts.isEmpty()) {
            "⚠️ حرکتت کم بود، دوباره امتحان کن"
        } else {
            "✅ تنظیم شد: " + parts.joinToString(" • ")
        }
        LiveState.wizardResultAt = now
        wizard = 0
        LiveState.wizardStep = 0
        LiveState.wizardMsg = ""
        armed = true
        pending = null
    }

    companion object {
        const val CHANNEL = "headscroll"
        const val ACTION_RECALIBRATE = "com.headscroll.RECALIBRATE"
        const val ACTION_TOGGLE_PAUSE = "com.headscroll.TOGGLE_PAUSE"
        const val ACTION_STOP = "com.headscroll.STOP"
        const val ACTION_WIZARD = "com.headscroll.WIZARD"
    }
}
