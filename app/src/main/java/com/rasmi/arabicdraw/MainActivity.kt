package com.rasmi.arabicdraw

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.graphics.*
import android.view.*
import android.widget.*
import java.io.File
import java.io.FileOutputStream

data class Category(val title: String, val emoji: String, val lessons: List<String>)
val categories = listOf(
    Category("حيوانات لطيفة", "🐱", listOf("قطة","أرنب","دب","فراشة","سمكة","عصفور")),
    Category("الطبيعة", "🌿", listOf("شجرة","زهرة","شمس","سحابة","جبال","صبار")),
    Category("طعام مرح", "🍓", listOf("تفاحة","فراولة","كعكة","آيس كريم","بيتزا","بطيخ")),
    Category("أشياء من حولنا", "🏡", listOf("منزل","مصباح","نافذة","كرسي","ساعة","مظلة")),
    Category("مركبات", "🚗", listOf("سيارة","حافلة","قارب","صاروخ","دراجة","طائرة")),
    Category("عالم البحار", "🐢", listOf("سلحفاة","أخطبوط","نجم البحر","دلفين","حوت","صدفة")),
    Category("خيال ومرح", "✨", listOf("تاج","عصا سحرية","روبوت","وحش لطيف","قلعة","هلال")),
    Category("الأشكال الأساسية", "🔷", listOf("دائرة","مربع","مثلث","قلب","نجمة","دوامة"))
)
class MainActivity : Activity() {
    private val purple = Color.rgb(106,77,188)
    private val paper = Color.rgb(255,250,244)
    private lateinit var root: LinearLayout
    private var selected = -1
    private lateinit var board: SketchBoard
    private var currentLesson = ""
    private var step = 1
    private val done by lazy { getSharedPreferences("progress", MODE_PRIVATE) }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = purple
        showHome()
    }
    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
    private fun start(title: String): LinearLayout {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(paper)
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16),dp(10),dp(16),dp(10))
        }
        val heading = TextView(this).apply {
            text = title; textSize = 26f; setTextColor(purple)
            gravity = Gravity.CENTER; setPadding(0,dp(12),0,dp(16))
            setTypeface(null,1)
        }
        root.addView(heading)
        setContentView(root)
        return root
    }
    private fun button(label: String, action: () -> Unit) {
        val b = Button(this).apply {
            text = label; textSize = 18f; isAllCaps = false
            setOnClickListener { action() }
        }
        root.addView(b, LinearLayout.LayoutParams(-1,dp(62)).apply { bottomMargin = dp(8) })
    }
    private fun scroll() {
        val old = root
        val wrapper = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setBackgroundColor(paper) }
        val scroller = ScrollView(this).apply { addView(old) }
        wrapper.addView(scroller)
        setContentView(wrapper)
    }
    private fun showHome() {
        selected = -1
        start("🎨 رسمة — تعلّم الرسم بالعربية")
        val intro = TextView(this).apply {
            text = "اختر ما تحب رسمه! كل الفئات والدروس مجانية ومفتوحة ✨"
            textSize = 18f; gravity = Gravity.CENTER
            setPadding(0,dp(8),0,dp(18))
        }
        root.addView(intro)
        categories.forEachIndexed { i, cat ->
            button("${cat.emoji}  ${cat.title}     •     ٦ دروس مفتوحة") { showCategory(i) }
        }
        button("🖼️ معرض رسوماتي") { showGallery() }
        scroll()
    }
    private fun showCategory(i: Int) {
        selected = i
        val cat = categories[i]
        start("${cat.emoji}  ${cat.title}")
        button("↩ الرجوع إلى الفئات") { showHome() }
        cat.lessons.forEachIndexed { index, title ->
            val key = "$i-$index"
            val check = if(done.getBoolean(key,false)) "✅" else "✏️"
            button("$check  المستوى ${index+1}: $title") { showLesson(i,index) }
        }
        scroll()
    }
    private fun showLesson(i: Int, level: Int) {
        selected = i
        currentLesson = categories[i].lessons[level]
        step = 1
        start("✏️  $currentLesson")
        val instruction = TextView(this).apply {
            textSize = 17f; gravity = Gravity.CENTER; text = "الخطوة ١ من ٤ — ابدأ بالأشكال الكبيرة"
        }
        root.addView(instruction)
        board = SketchBoard(this)
        root.addView(board, LinearLayout.LayoutParams(-1,0,1f).apply { topMargin=dp(8); bottomMargin=dp(8) })
        fun updateStep() {
            val tips = listOf("ابدأ بالأشكال الكبيرة","أضف الخطوط الرئيسية","ارسم التفاصيل الصغيرة","أكمل رسمتك ولوّنها")
            instruction.text = "الخطوة $step من ٤ — ${tips[step-1]}"
            board.step = step; board.invalidate()
        }
        val toolbar = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity=Gravity.CENTER }
        fun tool(label:String, action:()->Unit) {
            val b=Button(this).apply {text=label;textSize=13f;isAllCaps=false;setOnClickListener{action()}}
            toolbar.addView(b,LinearLayout.LayoutParams(0,dp(52),1f))
        }
        tool("التالي") { if(step<4)step++;updateStep() }
        tool("السابق") { if(step>1)step--;updateStep() }
        tool("تراجع") { board.undo() }
        tool("مسح") { board.clear() }
        root.addView(toolbar)
        val palette = LinearLayout(this).apply { orientation=LinearLayout.HORIZONTAL; gravity=Gravity.CENTER }
        val colors = listOf(Color.BLACK,Color.rgb(111,78,190),Color.RED,Color.BLUE,Color.rgb(30,130,60))
        colors.forEach { c ->
            val swatch = TextView(this).apply {
                text="●"; textSize=32f; gravity=Gravity.CENTER; setTextColor(c)
                setOnClickListener { board.ink=c }
            }
            palette.addView(swatch,LinearLayout.LayoutParams(0,dp(45),1f))
        }
        root.addView(palette)
        val actions = LinearLayout(this).apply {orientation=LinearLayout.HORIZONTAL}
        fun action(label:String, callback:()->Unit) {
            val b=Button(this).apply {text=label;isAllCaps=false;setOnClickListener{callback()}}
            actions.addView(b,LinearLayout.LayoutParams(0,dp(58),1f))
        }
        action("💾 حفظ الرسم") {
            val folder=File(filesDir,"drawings").apply{mkdirs()}
            val file=File(folder,"drawing_${System.currentTimeMillis()}.png")
            board.save(file)
            done.edit().putBoolean("$i-$level",true).apply()
            Toast.makeText(this,"تم حفظ رسمتك بنجاح!",Toast.LENGTH_SHORT).show()
        }
        action("↩ الدروس") { showCategory(i) }
        root.addView(actions)
    }
    private fun showGallery() {
        start("🖼️ معرض رسوماتي")
        button("↩ العودة") { showHome() }
        val files = File(filesDir,"drawings").listFiles()?.sortedByDescending{it.lastModified()} ?: emptyList()
        if(files.isEmpty()) button("لم تحفظ أي رسمة بعد"){}
        files.forEach { file ->
            button("🎨 ${file.name}") {
                val image=ImageView(this).apply{
                    setImageBitmap(BitmapFactory.decodeFile(file.absolutePath))
                    adjustViewBounds=true
                }
                AlertDialog.Builder(this).setTitle("رسمتي").setView(image).setPositiveButton("إغلاق",null).show()
            }
        }
        scroll()
    }
    @Deprecated("Deprecated in Android API")
    override fun onBackPressed() {
        if(selected>=0) showCategory(selected) else showHome()
    }
}

class SketchBoard(context: android.content.Context): View(context) {
    data class Stroke(val path: Path, val color: Int)
    private val strokes = mutableListOf<Stroke>()
    private var active: Path? = null
    var ink: Int = Color.BLACK
    var step: Int = 1
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply{
        style=Paint.Style.STROKE;strokeWidth=8f;strokeCap=Paint.Cap.ROUND;strokeJoin=Paint.Join.ROUND
    }
    init { setBackgroundColor(Color.WHITE) }
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawColor(Color.WHITE)
        val cx=width/2f;val cy=height/2f;val size=kotlin.math.min(width,height)*0.3f
        paint.color=Color.rgb(215,210,230);paint.strokeWidth=5f
        canvas.drawCircle(cx,cy,size,paint)
        if(step>=2) canvas.drawOval(cx-size*.75f,cy-size*.35f,cx+size*.75f,cy+size*.8f,paint)
        if(step>=3) {
            canvas.drawCircle(cx-size*.3f,cy-size*.1f,9f,paint)
            canvas.drawCircle(cx+size*.3f,cy-size*.1f,9f,paint)
        }
        if(step>=4)canvas.drawArc(cx-size*.3f,cy,cx+size*.3f,cy+size*.5f,0f,180f,false,paint)
        paint.strokeWidth=8f
        strokes.forEach { paint.color=it.color;canvas.drawPath(it.path,paint) }
    }
    override fun onTouchEvent(e: MotionEvent): Boolean {
        when(e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent?.requestDisallowInterceptTouchEvent(true)
                active=Path().apply{moveTo(e.x,e.y);lineTo(e.x+.1f,e.y+.1f)}
                strokes.add(Stroke(active!!,ink));invalidate();return true
            }
            MotionEvent.ACTION_MOVE -> {active?.lineTo(e.x,e.y);invalidate();return true}
            MotionEvent.ACTION_UP -> {active?.lineTo(e.x,e.y);active=null;invalidate();return true}
        }
        return true
    }
    fun undo(){if(strokes.isNotEmpty()){strokes.removeAt(strokes.lastIndex);invalidate()}}
    fun clear(){strokes.clear();invalidate()}
    fun save(file: File){
        val bitmap=Bitmap.createBitmap(width.coerceAtLeast(1),height.coerceAtLeast(1),Bitmap.Config.ARGB_8888)
        val canvas=Canvas(bitmap)
        onDraw(canvas)
        FileOutputStream(file).use{ bitmap.compress(Bitmap.CompressFormat.PNG,100,it)}
        bitmap.recycle()
    }
}
