package com.headscroll

import android.Manifest
import android.animation.ValueAnimator
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    companion object {
        var currentTab = 0
    }

    private lateinit var prefs: Prefs
    private lateinit var ui: Ui
    private val handler = Handler(Looper.getMainLooper())

    private var pages: List<ScrollView> = emptyList()
    private var pageCols: List<LinearLayout> = emptyList()
    private var tabViews: List<TextView> = emptyList()

    // خانه
    private lateinit var statusTv: TextView
    private lateinit var statusSub: TextView
    private lateinit var pulseDot: View
    private lateinit var toggleBtn: TextView
    private lateinit var pauseBtn: TextView
    private lateinit var lastTv: TextView
    private lateinit var todayTv: TextView
    private lateinit var totalTv: TextView
    private lateinit var setupCard: LinearLayout
    private lateinit var accState: TextView
    private lateinit var camState: TextView
    private lateinit var notifState: View
    private lateinit var notifStateTv: TextView
    private lateinit var handStatusTv: TextView
    private lateinit var pulse: ValueAnimator

    private var lastSeenGesture = 0L
    private var shownToday = 0
    private var shownTotal = 0
    private var startAfterPerm = false

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (startAfterPerm) {
                if (hasCamera()) startFace()
                else Toast.makeText(this, "بدون دسترسی دوربین کار نمیکنه", Toast.LENGTH_LONG).show()
            }
            startAfterPerm = false
            refresh()
        }

    private fun hasCamera() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun hasNotif() = Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        ui = Ui(this)

        val home = buildHome()
        val gest = buildGestures()
        val sett = buildSettings()
        pages = listOf(home.first, gest.first, sett.first)
        pageCols = listOf(home.second, gest.second, sett.second)

        val frame = FrameLayout(this)
        pages.forEach { frame.addView(it, FrameLayout.LayoutParams(MATCH, MATCH)) }

        val nav = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            setBackgroundColor(C.CARD)
            setPadding(0, ui.dp(4), 0, ui.dp(4))
        }
        val tabs = listOf("🏠" to "خانه", "🎯" to "ژست‌ها", "⚙️" to "تنظیمات")
        tabViews = tabs.mapIndexed { i, p ->
            ui.label(p.first + "\n" + p.second, 12f, C.SUB, true).apply {
                gravity = Gravity.CENTER
                textAlignment = View.TEXT_ALIGNMENT_CENTER
                setPadding(0, ui.dp(6), 0, ui.dp(6))
                isClickable = true
                setOnClickListener { showTab(i, true) }
                ui.pressable(this)
            }
        }
        tabViews.forEach { nav.addView(it, LinearLayout.LayoutParams(0, WRAP, 1f)) }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(C.BG)
        }
        root.addView(frame, LinearLayout.LayoutParams(MATCH, 0, 1f))
        root.addView(nav, LinearLayout.LayoutParams(MATCH, WRAP))

        val overlay = FrameLayout(this)
        overlay.addView(root, FrameLayout.LayoutParams(MATCH, MATCH))
        setContentView(overlay)

        showTab(currentTab, false)

        val autostart = intent.getBooleanExtra("autostart", false)
        if (autostart && hasCamera()) {
            startFace()
            finish()
            return
        }
        if (savedInstanceState == null && !autostart) showSplash(overlay)
        else ui.stagger(pageCols[currentTab])
    }

    // ---------- اسپلش ----------

    private fun showSplash(overlay: FrameLayout) {
        val splash = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(C.BG)
            isClickable = true
        }
        val logo = LogoView(this)
        splash.addView(logo, LinearLayout.LayoutParams(ui.dp(120), ui.dp(120)))
        val name = ui.label("HeadScroll", 28f, C.TEXT, true).apply {
            textAlignment = View.TEXT_ALIGNMENT_CENTER
            setPadding(0, ui.dp(18), 0, 0)
        }
        val tag = ui.label("سرتو تکون بده، ریلز عوض شه", 14f, C.SUB).apply {
            textAlignment = View.TEXT_ALIGNMENT_CENTER
        }
        splash.addView(name)
        splash.addView(tag)
        overlay.addView(splash, FrameLayout.LayoutParams(MATCH, MATCH))

        logo.scaleX = 0.5f; logo.scaleY = 0.5f; logo.alpha = 0f
        logo.animate().scaleX(1f).scaleY(1f).alpha(1f).setDuration(650)
            .setInterpolator(OvershootInterpolator(1.6f)).start()
        name.alpha = 0f; tag.alpha = 0f
        name.animate().alpha(1f).setStartDelay(350).setDuration(450).start()
        tag.animate().alpha(1f).setStartDelay(550).setDuration(450).start()
        handler.postDelayed({
            splash.animate().alpha(0f).setDuration(380).withEndAction {
                overlay.removeView(splash)
                ui.stagger(pageCols[currentTab])
            }.start()
        }, 1500)
    }

    // ---------- تب‌ها ----------

    private fun showTab(i: Int, animate: Boolean) {
        currentTab = i
        pages.forEachIndexed { j, p -> p.visibility = if (i == j) View.VISIBLE else View.GONE }
        tabViews.forEachIndexed { j, t ->
            t.setTextColor(if (i == j) C.ACCENT2 else C.SUB)
            if (animate && i == j) {
                t.scaleX = 0.85f; t.scaleY = 0.85f
                t.animate().scaleX(1f).scaleY(1f).setDuration(260)
                    .setInterpolator(OvershootInterpolator(2f)).start()
            }
        }
        if (animate) ui.stagger(pageCols[i])
    }

    private fun page(): Pair<ScrollView, LinearLayout> {
        val col = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(ui.dp(16), ui.dp(22), ui.dp(16), ui.dp(24))
        }
        val sv = ScrollView(this).apply {
            clipToPadding = false
            addView(col, ViewGroup.LayoutParams(MATCH, WRAP))
        }
        return Pair(sv, col)
    }

    // ---------- خانه ----------

    private fun buildHome(): Pair<ScrollView, LinearLayout> {
        val (sv, col) = page()

        val head = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        head.addView(LogoView(this), LinearLayout.LayoutParams(ui.dp(60), ui.dp(60)))
        val ht = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPaddingRelative(ui.dp(14), 0, 0, 0)
        }
        ht.addView(ui.label("HeadScroll", 26f, C.TEXT, true))
        ht.addView(ui.label("ریلز رو با سر و دست کنترل کن", 13f, C.SUB))
        head.addView(ht)
        col.addView(head, ui.vlp(0, 16))

        // کارت وضعیت
        val sc = ui.card()
        val srow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        pulseDot = View(this).apply {
            background = GradientDrawable().apply { shape = GradientDrawable.OVAL; setColor(C.SUB) }
        }
        srow.addView(pulseDot, LinearLayout.LayoutParams(ui.dp(12), ui.dp(12)))
        statusTv = ui.label("", 22f, C.SUB, true).apply { setPaddingRelative(ui.dp(10), 0, 0, 0) }
        srow.addView(statusTv)
        sc.addView(srow)
        statusSub = ui.label("", 13f, C.SUB)
        sc.addView(statusSub, ui.vlp(4, 12))
        toggleBtn = ui.button("") { toggle() }
        sc.addView(toggleBtn)
        pauseBtn = ui.button("", false) { sendToService(FaceService.ACTION_TOGGLE_PAUSE) }
        sc.addView(pauseBtn, ui.vlp(8, 0))
        col.addView(sc, ui.vlp(0, 12))

        pulse = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 1400
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener {
                val v = it.animatedValue as Float
                pulseDot.scaleX = 1f + 0.7f * v
                pulseDot.scaleY = 1f + 0.7f * v
                pulseDot.alpha = 1f - 0.75f * v
            }
        }

        // تست زنده
        val gc = ui.card()
        gc.addView(ui.label("🎯 تست زنده", 16f, C.TEXT, true))
        gc.addView(GaugeView(this, prefs, ui), ui.vlp(10, 10))
        lastTv = ui.label("", 14f, C.TEXT, true)
        gc.addView(lastTv)
        gc.addView(ui.button("✨ کالیبراسیون هوشمند", false) { startWizard() }, ui.vlp(12, 0))
        col.addView(gc, ui.vlp(0, 12))

        // راه‌اندازی
        setupCard = ui.card()
        setupCard.addView(ui.label("🚀 راه‌اندازی", 16f, C.TEXT, true))
        accState = setupRow(setupCard, "دسترسی Accessibility", "برای زدن سوایپ و تپ") {
            startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        camState = setupRow(setupCard, "دسترسی دوربین", "برای دیدن سر و دستت") {
            startAfterPerm = false
            permLauncher.launch(arrayOf(Manifest.permission.CAMERA))
        }
        notifStateTv = setupRow(setupCard, "اعلان", "برای نوتیفیکیشن کنترل") {
            if (Build.VERSION.SDK_INT >= 33) {
                startAfterPerm = false
                permLauncher.launch(arrayOf(Manifest.permission.POST_NOTIFICATIONS))
            }
        }
        notifState = notifStateTv.parent as View
        setupCard.addView(
            ui.label(
                "اگه اندروید ۱۳+ داری و Accessibility خاکستریه: اطلاعات برنامه ← سه نقطه ← Allow restricted settings",
                12f, C.SUB
            ), ui.vlp(8, 0)
        )
        col.addView(setupCard, ui.vlp(0, 12))

        // آمار
        val st = ui.card()
        st.addView(ui.label("📊 آمار", 16f, C.TEXT, true))
        val srow2 = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun stat(title: String): TextView {
            val c = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
            val v = ui.label("0", 30f, C.ACCENT2, true)
            c.addView(v)
            c.addView(ui.label(title, 12f, C.SUB))
            srow2.addView(c, LinearLayout.LayoutParams(0, WRAP, 1f))
            return v
        }
        todayTv = stat("ژست امروز")
        totalTv = stat("کل ژست‌ها")
        st.addView(srow2, ui.vlp(8, 0))
        col.addView(st, ui.vlp(0, 0))

        return Pair(sv, col)
    }

    private fun setupRow(parent: LinearLayout, title: String, sub: String, onClick: () -> Unit): TextView {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setPadding(0, ui.dp(10), 0, ui.dp(10))
            isClickable = true
            setOnClickListener { onClick() }
        }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(ui.label(title, 15f))
        col.addView(ui.label(sub, 12f, C.SUB))
        row.addView(col, LinearLayout.LayoutParams(0, WRAP, 1f))
        val state = ui.label("⭕", 20f)
        row.addView(state)
        ui.pressable(row)
        parent.addView(row)
        return state
    }

    // ---------- ژست‌ها ----------

    private fun buildGestures(): Pair<ScrollView, LinearLayout> {
        val (sv, col) = page()
        col.addView(ui.label("ژست‌ها 🎯", 24f, C.TEXT, true), ui.vlp(0, 4))
        col.addView(ui.label("برای هر ژست مشخص کن چه اتفاقی بیفته", 13f, C.SUB), ui.vlp(0, 14))

        // پریست‌ها
        val pc = ui.card()
        pc.addView(ui.label("حساسیت آماده", 16f, C.TEXT, true))
        val prow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        listOf("🐢 نرم", "⚖️ معمولی", "⚡ حساس").forEachIndexed { i, name ->
            val b = ui.button(name, false) {
                prefs.applyPreset(i)
                recreate()
            }
            prow.addView(b, LinearLayout.LayoutParams(0, WRAP, 1f).apply {
                setMargins(ui.dp(3), 0, ui.dp(3), 0)
            })
        }
        pc.addView(prow, ui.vlp(10, 0))
        col.addView(pc, ui.vlp(0, 12))

        // اکشن‌ها
        val ac = ui.card()
        ac.addView(ui.label("حرکت سر و صورت", 16f, C.TEXT, true))
        Trigger.values().filter { !it.hand }.forEach { t -> ac.addView(triggerRow(t)) }
        ac.addView(ui.divider(), ui.vlp(6, 6))
        ac.addView(ui.label("ژست دست", 16f, C.TEXT, true), ui.vlp(6, 0))
        handStatusTv = ui.label("", 12f, C.SUB)
        ac.addView(handStatusTv, ui.vlp(2, 2))
        Trigger.values().filter { it.hand }.forEach { t -> ac.addView(triggerRow(t)) }
        col.addView(ac, ui.vlp(0, 12))

        // اسلایدرها
        val sc = ui.card()
        sc.addView(ui.label("تنظیم دقیق", 16f, C.TEXT, true))
        sc.addView(ui.sliderRow("زاویه بالا/پایین", 8, 30, 1, prefs.pitchTh, { "$it°" }) { prefs.pitchTh = it })
        sc.addView(ui.sliderRow("زاویه چپ/راست", 10, 40, 1, prefs.yawTh, { "$it°" }) { prefs.yawTh = it })
        sc.addView(ui.sliderRow("مدت نگه داشتن ژست", 0, 500, 50, prefs.holdMs, { "$it ms" }) { prefs.holdMs = it })
        sc.addView(ui.sliderRow("فاصله بین دو ژست", 500, 3000, 100, prefs.cooldownMs,
            { "%.1f ثانیه".format(it / 1000f) }) { prefs.cooldownMs = it })
        col.addView(sc, ui.vlp(0, 12))

        // کالیبراسیون
        val cc = ui.card()
        cc.addView(ui.label("کالیبراسیون", 16f, C.TEXT, true))
        cc.addView(ui.switchRow("کالیبراسیون خودکار", "وقتی چهره برمیگرده یا اپ عوض میشه، حالت عادی سرت دوباره یاد گرفته میشه",
            prefs.autoCal) { prefs.autoCal = it })
        cc.addView(ui.switchRow("برعکس کردن بالا/پایین", "اگه جهت‌ها برعکس بود", prefs.invertV) { prefs.invertV = it })
        cc.addView(ui.switchRow("برعکس کردن چپ/راست", null, prefs.invertH) { prefs.invertH = it })
        cc.addView(ui.button("✨ کالیبراسیون هوشمند (۱۵ ثانیه)") { startWizard() }, ui.vlp(10, 0))
        cc.addView(ui.button("🎯 کالیبره سریع", false) { sendToService(FaceService.ACTION_RECALIBRATE) }, ui.vlp(8, 0))
        col.addView(cc, ui.vlp(0, 0))

        return Pair(sv, col)
    }

    private fun triggerRow(t: Trigger): View {
        val sub = when (t) {
            Trigger.SMILE -> "لبخند زدن"
            Trigger.HAND_OK -> "انگشت شست و اشاره به هم، بقیه باز"
            Trigger.HAND_PINCH -> "شست و اشاره به هم، بقیه جمع"
            Trigger.HAND_PALM -> "کف دست باز جلوی دوربین"
            Trigger.HAND_VICTORY -> "دو انگشت اشاره و وسط"
            else -> null
        }
        return ui.choiceRow(t.emoji, t.label, prefs.action(t).label, sub) { valueTv ->
            val all = Action.values()
            ui.pickDialog("${t.emoji} ${t.label} → چی بشه؟", all.map { it.label }) { i ->
                prefs.setAction(t, all[i])
                valueTv.text = all[i].label
            }
        }
    }

    // ---------- تنظیمات ----------

    private fun buildSettings(): Pair<ScrollView, LinearLayout> {
        val (sv, col) = page()
        col.addView(ui.label("تنظیمات ⚙️", 24f, C.TEXT, true), ui.vlp(0, 14))

        val tc = ui.card()
        tc.addView(ui.label("اپ‌های هدف", 16f, C.TEXT, true))
        Targets.all.forEach { app ->
            tc.addView(ui.switchRow(app.label, null, prefs.targets.contains(app.key)) { on ->
                val s = prefs.targets.toMutableSet()
                if (on) s.add(app.key) else s.remove(app.key)
                prefs.targets = s
            })
        }
        tc.addView(ui.divider(), ui.vlp(4, 4))
        tc.addView(ui.switchRow("همه اپ‌ها", "احتیاط: تو هر اپی سوایپ میزنه", prefs.allApps) { prefs.allApps = it })
        col.addView(tc, ui.vlp(0, 12))

        val bc = ui.card()
        bc.addView(ui.label("رفتار", 16f, C.TEXT, true))
        bc.addView(ui.sliderRow("سرعت سوایپ", 100, 600, 50, prefs.swipeMs, { "$it ms" }) { prefs.swipeMs = it })
        bc.addView(ui.switchRow("دوربین هوشمند", "فقط وقتی اپ هدف بازه دوربین روشن میشه (باتری کمتر)",
            prefs.smartCamera) { prefs.smartCamera = it })
        bc.addView(ui.switchRow("لرزش", "با هر ژست یه لرزش کوتاه", prefs.vibrate) { prefs.vibrate = it })
        col.addView(bc, ui.vlp(0, 12))

        val oc = ui.card()
        oc.addView(ui.label("بیشتر", 16f, C.TEXT, true))
        oc.addView(ui.button("🔋 رفع محدودیت باتری", false) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }, ui.vlp(8, 0))
        oc.addView(ui.button("♻️ ریست تنظیمات", false) {
            AlertDialog.Builder(this, android.R.style.Theme_Material_Dialog_Alert)
                .setTitle("همه تنظیمات به حالت اول برگرده؟")
                .setPositiveButton("آره") { _, _ -> prefs.resetDefaults(); recreate() }
                .setNegativeButton("نه", null)
                .show()
        }, ui.vlp(8, 0))
        val ver = runCatching { packageManager.getPackageInfo(packageName, 0).versionName }.getOrNull() ?: ""
        oc.addView(ui.button("🌐 HeadScroll v$ver • گیت‌هاب", false) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/TheROMZ52/headscroll")))
        }, ui.vlp(8, 0))
        col.addView(oc, ui.vlp(0, 0))

        return Pair(sv, col)
    }

    // ---------- اکشن‌ها ----------

    private fun toggle() {
        if (LiveState.running) {
            stopService(Intent(this, FaceService::class.java))
        } else {
            requestStart()
        }
        handler.postDelayed({ refresh() }, 350)
    }

    private fun requestStart() {
        if (hasCamera()) {
            startFace()
        } else {
            startAfterPerm = true
            val perms = mutableListOf(Manifest.permission.CAMERA)
            if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
            permLauncher.launch(perms.toTypedArray())
        }
    }

    private fun startFace() {
        ContextCompat.startForegroundService(this, Intent(this, FaceService::class.java))
    }

    private fun sendToService(action: String) {
        if (!LiveState.running) {
            Toast.makeText(this, "اول روشنش کن", Toast.LENGTH_SHORT).show()
            return
        }
        startService(Intent(this, FaceService::class.java).setAction(action))
    }

    private fun startWizard() {
        if (!hasCamera()) {
            requestStart()
            return
        }
        if (!LiveState.running) {
            startFace()
            handler.postDelayed({
                startService(Intent(this, FaceService::class.java).setAction(FaceService.ACTION_WIZARD))
            }, 900)
        } else {
            startService(Intent(this, FaceService::class.java).setAction(FaceService.ACTION_WIZARD))
        }
        showTab(0, true)
    }

    // ---------- بروزرسانی UI ----------

    private val loop = object : Runnable {
        override fun run() {
            refresh()
            handler.postDelayed(this, 250)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(loop)
    }

    override fun onPause() {
        handler.removeCallbacks(loop)
        if (pulse.isRunning) pulse.cancel()
        super.onPause()
    }

    private fun animateNumber(tv: TextView, from: Int, to: Int) {
        ValueAnimator.ofInt(from, to).apply {
            duration = 450
            addUpdateListener { tv.text = (it.animatedValue as Int).toString() }
            start()
        }
    }

    private fun refresh() {
        val acc = ScrollAccessibilityService.instance != null
        val cam = hasCamera()
        val notif = hasNotif()
        accState.text = if (acc) "✅" else "⭕"
        camState.text = if (cam) "✅" else "⭕"
        notifStateTv.text = if (notif) "✅" else "⭕"
        (notifState).visibility = if (Build.VERSION.SDK_INT >= 33) View.VISIBLE else View.GONE
        setupCard.visibility = if (acc && cam && notif) View.GONE else View.VISIBLE

        val running = LiveState.running
        val paused = LiveState.paused
        val color = when {
            !running -> C.SUB
            paused -> C.WARN
            else -> C.GOOD
        }
        statusTv.text = when {
            !running -> "خاموش"
            paused -> "مکث شده"
            else -> "روشن و آماده"
        }
        statusTv.setTextColor(color)
        (pulseDot.background as GradientDrawable).setColor(color)
        statusSub.text = when {
            !running -> "دکمه‌ی پایین رو بزن تا شروع شه"
            paused -> "دوربین خاموشه تا ادامه بدی"
            !acc -> "⚠️ Accessibility فعال نیست، سوایپ زده نمیشه"
            LiveState.cameraOn -> "دوربین فعاله؛ اپ هدف رو باز کن"
            else -> "دوربین خاموشه تا اپ هدف باز شه"
        }
        val active = running && !paused
        if (active && !pulse.isRunning) pulse.start()
        if (!active && pulse.isRunning) {
            pulse.cancel()
            pulseDot.scaleX = 1f; pulseDot.scaleY = 1f; pulseDot.alpha = 1f
        }
        toggleBtn.text = if (running) "⏹ خاموش کردن" else "▶ روشن کردن"
        pauseBtn.visibility = if (running) View.VISIBLE else View.GONE
        pauseBtn.text = if (paused) "▶ ادامه" else "⏸ مکث"

        if (LiveState.lastGestureAt != lastSeenGesture) {
            lastSeenGesture = LiveState.lastGestureAt
            lastTv.text = LiveState.lastGesture
            lastTv.scaleX = 1.12f; lastTv.scaleY = 1.12f
            lastTv.animate().scaleX(1f).scaleY(1f).setDuration(350)
                .setInterpolator(OvershootInterpolator(2.5f)).start()
        } else if (lastTv.text.isEmpty()) {
            lastTv.text = "هنوز ژستی تشخیص داده نشده"
        }

        val td = prefs.countToday()
        val tt = prefs.countTotal()
        if (td != shownToday) { animateNumber(todayTv, shownToday, td); shownToday = td }
        if (tt != shownTotal) { animateNumber(totalTv, shownTotal, tt); shownTotal = tt }

        handStatusTv.text = when {
            !running -> "سرویس رو روشن کن تا وضعیت تشخیص دست معلوم شه"
            LiveState.handReady -> "✅ تشخیص دست آماده‌ست"
            LiveState.handError -> "⚠️ مدل تشخیص دست لود نشد (نسخه‌ی GitHub Actions رو نصب کن)"
            else -> "⏳ در حال لود مدل دست…"
        }
    }
}
