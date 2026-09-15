package com.example.v2raytoclash

import android.animation.ObjectAnimator
import android.app.Activity
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.*
import java.io.File
import kotlin.math.roundToInt

class MainActivity : Activity() {

    private lateinit var input: EditText
    private lateinit var log: TextView
    private lateinit var progress: ProgressBar
    private lateinit var status: TextView
    private lateinit var rootStatus: TextView
    private lateinit var transferButton: Button

    private val bgColor = Color.rgb(5, 10, 18)
    private val cardColor = Color.rgb(14, 23, 38)
    private val card2 = Color.rgb(18, 29, 47)
    private val inputColor = Color.rgb(7, 15, 27)
    private val cyan = Color.rgb(0, 190, 255)
    private val purple = Color.rgb(116, 76, 255)
    private val orange = Color.rgb(255, 167, 48)
    private val red = Color.rgb(238, 80, 105)
    private val green = Color.rgb(46, 220, 133)
    private val white = Color.WHITE
    private val muted = Color.rgb(145, 161, 185)
    private var pulse: ObjectAnimator? = null

    private fun dp(v: Int): Int = (v * resources.displayMetrics.density).roundToInt()

    private fun rounded(color: Int, radiusDp: Float = 18f, stroke: Int? = null): GradientDrawable =
        GradientDrawable().apply {
            setColor(color)
            cornerRadius = dp(radiusDp.toInt()).toFloat()
            if (stroke != null) setStroke(dp(1), stroke)
        }

    private fun button(text: String, color: Int): Button = Button(this).apply {
        this.text = text
        setTextColor(white)
        textSize = 14f
        isAllCaps = false
        gravity = Gravity.CENTER
        background = rounded(color, 16f)
        stateListAnimator = null
        minHeight = 0
        minimumHeight = 0
        includeFontPadding = false
        typeface = Typeface.DEFAULT_BOLD
        setPadding(dp(12), 0, dp(12), 0)
    }

    private fun lp(w: Int = -1, h: Int = -2, top: Int = 0, bottom: Int = 0): LinearLayout.LayoutParams =
        LinearLayout.LayoutParams(w, h).apply {
            setMargins(0, dp(top), 0, dp(bottom))
        }

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        window.statusBarColor = bgColor
        window.navigationBarColor = bgColor
        window.decorView.systemUiVisibility = 0

        val scroll = ScrollView(this).apply {
            isFillViewport = true
            clipToPadding = false
            setPadding(0, 0, 0, dp(8))
        }

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.TOP
            setPadding(dp(12), dp(8), dp(12), dp(16))
            setBackgroundColor(bgColor)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        scroll.addView(root)

        buildHeader(root)
        buildRootCard(root)
        buildProtocolCards(root)
        buildInputCard(root)
        buildActionCard(root)
        buildStatusCard(root)
        buildFooter(root)

        setContentView(scroll)
    }

    private fun textView(
        text: String,
        size: Float,
        color: Int,
        bold: Boolean = false,
        gravity: Int = Gravity.CENTER_VERTICAL
    ) = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(color)
        this.gravity = gravity
        includeFontPadding = false
        typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
    }

    private fun buildHeader(root: LinearLayout) {
        val header = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(0, dp(4), 0, dp(4))
        }

        header.addView(LogoView(this), LinearLayout.LayoutParams(dp(56), dp(56)))

        val texts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(10), 0, 0, 0)
        }

        texts.addView(textView("SELOOM1", 22f, white, true), lp())
        texts.addView(textView("V2Ray  →  Clash / Mihomo", 12f, cyan, true), lp(top = 3))
        texts.addView(textView("تحويل ونقل مباشر إلى BoxProxy", 10f, muted), lp(top = 3))

        header.addView(texts, LinearLayout.LayoutParams(0, dp(56), 1f))
        root.addView(header, lp(h = dp(64), bottom = 2))

        root.addView(View(this).apply {
            setBackgroundColor(Color.rgb(30, 53, 78))
        }, lp(h = dp(1), top = 2, bottom = 10))
    }

    private fun buildRootCard(root: LinearLayout) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            background = rounded(cardColor, 16f, Color.rgb(31, 53, 78))
            setPadding(dp(12), dp(8), dp(8), dp(8))
        }

        val labels = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        labels.addView(textView("صلاحيات Root", 13f, white, true), lp(h = dp(22)))
        rootStatus = textView("جاري فحص الصلاحية...", 10.5f, muted, false)
        labels.addView(rootStatus, lp(top = 2, h = dp(20)))
        card.addView(labels, LinearLayout.LayoutParams(0, dp(42), 1f))

        val refresh = button("فحص", Color.rgb(38, 61, 88)).apply {
            textSize = 11f
            setOnClickListener { checkRootStatus() }
        }
        card.addView(refresh, LinearLayout.LayoutParams(dp(62), dp(38)))
        root.addView(card, lp(h = dp(58), bottom = 10))
        checkRootStatus()
    }

    private fun checkRootStatus() {
        rootStatus.text = "جاري الفحص..."
        rootStatus.setTextColor(muted)
        Thread {
            val result = RootBridge.check()
            runOnUiThread {
                if (result.ok && result.output.contains("uid=0")) {
                    rootStatus.text = "متصل ✓  — جاهز للنقل إلى BoxProxy"
                    rootStatus.setTextColor(green)
                } else {
                    rootStatus.text = "غير متصل — امنح التطبيق صلاحية Root"
                    rootStatus.setTextColor(Color.rgb(255, 167, 48))
                }
            }
        }.start()
    }

    private fun buildProtocolCards(root: LinearLayout) {
        root.addView(textView("البروتوكولات المدعومة (4)", 14f, white, true), lp(h = dp(24), bottom = 6))

        val horizontal = HorizontalScrollView(this).apply {
            isHorizontalScrollBarEnabled = false
            overScrollMode = View.OVER_SCROLL_NEVER
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            clipToPadding = false
        }

        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_LTR
        }

        val protocols = listOf(
            Triple("VLESS", "V", cyan),
            Triple("VMess", "M", purple),
            Triple("Trojan", "T", orange),
            Triple("WireGuard", "W", red)
        )

        protocols.forEach { (name, icon, color) ->
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = rounded(cardColor, 14f, Color.rgb(31, 53, 78))
                setPadding(dp(8), dp(7), dp(8), dp(7))
            }

            card.addView(textView(icon, 18f, color, true, Gravity.CENTER), lp(h = dp(25)))
            card.addView(textView(name, 11f, white, true, Gravity.CENTER), lp(top = 4, h = dp(22)))

            row.addView(
                card,
                LinearLayout.LayoutParams(dp(100), dp(56)).apply {
                    setMargins(dp(3), 0, dp(3), 0)
                }
            )
        }

        horizontal.addView(row, FrameLayout.LayoutParams(-2, dp(60)))
        root.addView(horizontal, lp(h = dp(60), bottom = 10))
    }

    private fun buildInputCard(root: LinearLayout) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(cardColor, 20f)
            setPadding(dp(12), dp(10), dp(12), dp(11))
        }

        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        heading.addView(
            textView("أدخل رابط البروتوكول", 14f, white, true),
            LinearLayout.LayoutParams(0, dp(28), 1f)
        )
        heading.addView(textView("رابط واحد", 10f, green, true))

        card.addView(heading)

        input = EditText(this).apply {
            hint = "vless://...  |  vmess://...  |  trojan://...  |  wireguard://..."
            setHintTextColor(Color.rgb(91, 111, 139))
            setTextColor(white)
            textSize = 14f
            gravity = Gravity.TOP or Gravity.START
            setPadding(dp(14), dp(12), dp(14), dp(12))
            background = rounded(inputColor, 16f, Color.rgb(29, 53, 80))
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            textDirection = View.TEXT_DIRECTION_LTR
            isSingleLine = false
            minLines = 3
            maxLines = 5
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_URI
            includeFontPadding = true
        }

        card.addView(input, lp(h = dp(104), top = 6, bottom = 2))

        root.addView(card, lp(bottom = 12))
    }

    private fun buildActionCard(root: LinearLayout) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(card2, 20f)
            setPadding(dp(10), dp(10), dp(10), dp(10))
        }

        val heading = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        heading.addView(
            textView("تنفيذ العملية", 14f, white, true),
            LinearLayout.LayoutParams(0, dp(26), 1f)
        )
        heading.addView(textView("إنشاء + نقل تلقائي", 11f, cyan, true))
        card.addView(heading, lp(h = dp(26), bottom = 6))

        transferButton = button("🚀  تحويل ونقل إلى BoxProxy", purple)
        val clear = button("مسح", Color.rgb(38, 51, 70))

        card.addView(transferButton, lp(h = dp(50), bottom = 6))
        card.addView(clear, lp(h = dp(38)))

        transferButton.setOnClickListener { process(true) }
        clear.setOnClickListener {
            input.text.clear()
            log.text = "السجل جاهز للعمل..."
            status.text = "جاهز"
            status.setTextColor(green)
            progress.visibility = View.GONE
            progress.progress = 0
        }

        root.addView(card, lp(bottom = 12))
    }

    private fun buildStatusCard(root: LinearLayout) {
        val card = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = rounded(cardColor, 20f)
            setPadding(dp(12), dp(10), dp(12), dp(10))
        }

        val top = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }

        status = textView("جاهز", 14f, green, true)
        top.addView(status, LinearLayout.LayoutParams(0, dp(30), 1f))
        top.addView(textView("حالة العملية", 13f, muted, true))
        card.addView(top)

        progress = ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal).apply {
            max = 100
            progress = 0
            visibility = View.GONE
        }
        card.addView(progress, lp(h = dp(5), top = 5, bottom = 8))

        log = textView("السجل جاهز للعمل...", 11.5f, Color.rgb(188, 203, 225))
        log.setPadding(dp(10), dp(10), dp(10), dp(10))
        log.background = rounded(Color.rgb(7, 14, 25), 14f)
        log.layoutDirection = View.LAYOUT_DIRECTION_LTR
        log.textDirection = View.TEXT_DIRECTION_LTR
        log.gravity = Gravity.TOP or Gravity.START
        log.setHorizontallyScrolling(false)
        card.addView(log, lp(h = dp(94)))

        root.addView(card, lp(bottom = 12))
    }

    private fun buildFooter(root: LinearLayout) {
        val footer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(0, dp(5), 0, dp(2))
        }

        footer.addView(textView(
            "SELOOM CONVERT MIHOMO  •  🇮🇶\nتحويل بروتوكولات سريع وآمن إلى BoxProxy\n© 2026  SELOOM1",
            10.5f, Color.rgb(146, 170, 201), true, Gravity.CENTER
        ), lp(bottom = 3))

        val telegram = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_LTR
            setPadding(dp(8), dp(4), dp(8), dp(4))
            isClickable = true
            setOnClickListener {
                try {
                    startActivity(android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://t.me/freevpsiraq")
                    ))
                } catch (_: Exception) {}
            }
        }

        telegram.addView(textView("✈️", 19f, Color.WHITE, false, Gravity.CENTER),
            LinearLayout.LayoutParams(dp(32), dp(32)))

        val tgTexts = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_VERTICAL
        }
        tgTexts.addView(textView("تابعنا على Telegram", 11f, white, true, Gravity.START))
        tgTexts.addView(textView("@freevpsiraq  •  تحديثات وأدوات", 10.5f, cyan, false, Gravity.START))

        telegram.addView(tgTexts, LinearLayout.LayoutParams(dp(145), dp(40)))
        footer.addView(telegram)

        root.addView(footer, lp(h = dp(82)))
    }

    private fun animateWorking() {
        status.text = "جاري التحويل..."
        status.setTextColor(cyan)
        progress.visibility = View.VISIBLE
        progress.progress = 10

        pulse?.cancel()
        pulse = ObjectAnimator.ofFloat(transferButton, View.ALPHA, 1f, 0.55f, 1f).apply {
            duration = 850
            repeatCount = ObjectAnimator.INFINITE
            interpolator = DecelerateInterpolator()
            start()
        }

        transferButton.isEnabled = false
    }

    private fun finishWorking(ok: Boolean) {
        pulse?.cancel()
        pulse = null
        transferButton.alpha = 1f
        transferButton.isEnabled = true
        progress.progress = if (ok) 100 else 0
        progress.visibility = View.GONE
        status.text = if (ok) "اكتملت العملية ✓" else "حدث خطأ ✗"
        status.setTextColor(if (ok) green else Color.rgb(255, 100, 120))
    }

    private fun process(move: Boolean) {
        val link = input.text.toString().trim()

        if (link.isBlank()) {
            toast("الصق رابط بروتوكول أولاً")
            return
        }
        if (link.contains("\n") || link.contains("\r")) {
            toast("هذا الإصدار يقبل رابطاً واحداً فقط")
            return
        }

        animateWorking()
        log.text = "⏳ جاري تحليل الرابط..."
        progress.progress = 25

        try {
            val proxy = Converter.convert(link)
            log.append("\n✓ البروتوكول: ${proxy.type.uppercase()}")
            log.append("\n✓ اسم التكوين: ${proxy.name}")
            progress.progress = 55

            val yaml = Converter.yaml(listOf(proxy))
            val safe = proxy.name.ifBlank { proxy.type }
            val dir = File(filesDir, "configs").apply { mkdirs() }
            val file = File(dir, "$safe.yaml")
            file.writeText(yaml, Charsets.UTF_8)

            log.append("\n✓ تم إنشاء: ${file.name}")
            progress.progress = 75

            if (move) {
                log.append("\n⏳ جاري طلب صلاحيات Root...")
                val result = RootBridge.copy(file)
                progress.progress = 95

                if (result.ok) {
                    log.append("\n✓ تم النقل بنجاح")
                    log.append("\n📁 ${result.output}")
                    finishWorking(true)
                    toast("تم التحويل والنقل بنجاح")
                } else {
                    log.append("\n✗ فشل النقل")
                    log.append("\n${result.output}")
                    finishWorking(false)
                    toast("تم إنشاء التكوين لكن فشل النقل")
                }
            } else {
                log.append("\n✓ التكوين جاهز داخل مجلد التطبيق")
                finishWorking(true)
                toast("تم إنشاء ${file.name}")
            }
        } catch (e: Exception) {
            log.text = "✗ حدث خطأ:\n${e.message ?: e}"
            finishWorking(false)
            toast("فشل التحويل")
        }
    }

    private fun toast(s: String) =
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show()

    private class LogoView(context: android.content.Context) : View(context) {
        private val p = Paint(Paint.ANTI_ALIAS_FLAG)

        private fun dp(v: Int): Float =
            (v * resources.displayMetrics.density)

        override fun onDraw(c: Canvas) {
            val cx = width / 2f
            val cy = height / 2f
            val r = minOf(width, height) * 0.40f

            val grad = LinearGradient(
                cx - r, cy - r, cx + r, cy + r,
                Color.rgb(0, 225, 210),
                Color.rgb(115, 65, 255),
                Shader.TileMode.CLAMP
            )
            p.shader = grad
            c.drawRoundRect(cx - r, cy - r, cx + r, cy + r, dp(18), dp(18), p)

            p.shader = null
            p.color = Color.rgb(7, 13, 24)
            c.drawCircle(cx, cy, r * 0.58f, p)

            p.style = Paint.Style.STROKE
            p.strokeWidth = dp(3)
            p.color = Color.WHITE
            c.drawCircle(cx, cy, r * 0.36f, p)

            p.style = Paint.Style.FILL
            p.textAlign = Paint.Align.CENTER
            p.typeface = Typeface.DEFAULT_BOLD
            p.textSize = r * 0.42f
            c.drawText("S", cx, cy + r * 0.15f, p)
        }
    }
}
