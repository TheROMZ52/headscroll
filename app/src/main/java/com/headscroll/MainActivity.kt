package com.headscroll

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.SeekBar
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    private lateinit var prefs: Prefs
    private lateinit var accTv: TextView
    private lateinit var toggleBtn: Button

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            if (hasCamera()) startFace()
            else Toast.makeText(this, "بدون دسترسی دوربین کار نمیکنه", Toast.LENGTH_LONG).show()
        }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun hasCamera() =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(20), dp(36), dp(20), dp(24))
        }

        fun label(text: String, size: Float = 15f) = TextView(this).apply {
            this.text = text
            textSize = size
            textAlignment = View.TEXT_ALIGNMENT_VIEW_START
            setPadding(0, dp(14), 0, dp(4))
        }

        root.addView(label("HeadScroll", 28f))
        root.addView(label("سرتو بده بالا = ریلز بعدی\nسرتو بده پایین = ریلز قبلی", 16f))

        accTv = label("")
        root.addView(accTv)
        root.addView(Button(this).apply {
            text = "باز کردن تنظیمات دسترسی (Accessibility)"
            setOnClickListener { startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
        })

        toggleBtn = Button(this).apply {
            setOnClickListener {
                if (FaceService.running) {
                    stopService(Intent(this@MainActivity, FaceService::class.java))
                    postDelayed({ refresh() }, 300)
                } else {
                    val perms = mutableListOf(Manifest.permission.CAMERA)
                    if (Build.VERSION.SDK_INT >= 33) perms.add(Manifest.permission.POST_NOTIFICATIONS)
                    if (hasCamera()) startFace() else permLauncher.launch(perms.toTypedArray())
                }
            }
        }
        root.addView(toggleBtn)

        root.addView(Button(this).apply {
            text = "کالیبره مجدد (حالت عادی سر)"
            setOnClickListener {
                if (FaceService.running) {
                    startService(
                        Intent(this@MainActivity, FaceService::class.java)
                            .setAction(FaceService.ACTION_RECALIBRATE)
                    )
                }
            }
        })

        val thTv = label("")
        val thBar = SeekBar(this).apply { max = 20; progress = prefs.threshold - 10 }
        fun updTh() { thTv.text = "زاویه تشخیص: ${prefs.threshold} درجه (کمتر = حساس‌تر)" }
        thBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                prefs.threshold = 10 + p; updTh()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        updTh()
        root.addView(thTv); root.addView(thBar)

        val cdTv = label("")
        val cdBar = SeekBar(this).apply { max = 30; progress = (prefs.cooldownMs - 500) / 100 }
        fun updCd() { cdTv.text = "فاصله بین دو ریلز: ${"%.1f".format(prefs.cooldownMs / 1000f)} ثانیه" }
        cdBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(sb: SeekBar?, p: Int, fromUser: Boolean) {
                prefs.cooldownMs = 500 + p * 100; updCd()
            }
            override fun onStartTrackingTouch(sb: SeekBar?) {}
            override fun onStopTrackingTouch(sb: SeekBar?) {}
        })
        updCd()
        root.addView(cdTv); root.addView(cdBar)

        root.addView(Switch(this).apply {
            text = "فقط داخل اینستاگرام کار کنه"
            isChecked = prefs.onlyInstagram
            setPadding(0, dp(18), 0, dp(8))
            setOnCheckedChangeListener { _, c -> prefs.onlyInstagram = c }
        })

        root.addView(label(
            "نکته: اگه اندروید ۱۳ به بالا داری و دسترسی Accessibility خاکستریه، برو تنظیمات > برنامه‌ها > HeadScroll > سه نقطه بالا > Allow restricted settings، بعد دوباره امتحان کن.",
            13f
        ))

        setContentView(ScrollView(this).apply {
            addView(root, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT))
        })
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    private fun startFace() {
        ContextCompat.startForegroundService(this, Intent(this, FaceService::class.java))
        toggleBtn.postDelayed({ refresh() }, 400)
    }

    private fun refresh() {
        val acc = ScrollAccessibilityService.instance != null
        accTv.text = if (acc) "✅ دسترسی Accessibility فعاله" else "❌ دسترسی Accessibility هنوز فعال نشده"
        toggleBtn.text = if (FaceService.running) "⏹ خاموش کردن" else "▶ روشن کردن"
    }
}
