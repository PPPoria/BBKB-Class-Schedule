package com.bbkb.sc.ui.widget

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.OverScroller
import androidx.core.animation.doOnEnd
import androidx.core.view.isGone
import androidx.core.view.isVisible
import com.bbkb.sc.R
import com.bbkb.sc.databinding.ItemTableCellBinding
import com.bbkb.sc.databinding.ItemTableXBinding
import com.bbkb.sc.databinding.ItemTableYBinding
import com.bbkb.sc.isNightModeYes
import com.bbkb.sc.schedule.TableAttr
import com.bbkb.sc.util.SCLog
import com.poria.base.ext.dp2px
import com.poria.base.ext.setOnClickListenerWithClickAnimation
import kotlin.math.abs


@SuppressLint("ViewConstructor")
class TableView : ViewGroup {
    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    constructor(
        context: Context?,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes)

    // 回调
    var onClickCell: (Cell) -> Unit = {}
    var onScrollToPre: (List<Cell>) -> Unit = {}
    var onScrollToNext: (List<Cell>) -> Unit = {}

    // 尺寸
    private val dayOfWeekTextSizeBase = 9f
    private val dateTextSizeBase = 8f
    private val xHBase = dp2px(43f)
    private val nodeNumberTextSizeBase = 9f
    private val timeTextSizeBase = 8f
    private val yWBase = dp2px(35f)
    private val nameTextSizeBase = 9f
    private val teacherAndRoomTextSizeBase = 8f
    private val tableHeightBase = dp2px(100f)
    private var attr: TableAttr = TableAttr.latest
    private val dayOfWeekTextSize: Float
        get() = dayOfWeekTextSizeBase * attr.xAxisDayOfWeekTextSizeScale
    private val dateTextSize: Float
        get() = dateTextSizeBase * attr.xAxisDateTextSizeScale
    private val xH: Float
        get() = xHBase * attr.xAxisHeightScale
    private val nodeNumberTextSize: Float
        get() = nodeNumberTextSizeBase * attr.yAxisNodeNumberTextSizeScale
    private val timeTextSize: Float
        get() = timeTextSizeBase * attr.yAxisTimeTexSizeStScale
    private val yW: Float
        get() = yWBase * attr.yAxisWidthScale
    private val nameTextSize: Float
        get() = nameTextSizeBase * attr.courseNameTextSizeScale
    private val teacherAndRoomTextSize: Float
        get() = teacherAndRoomTextSizeBase * attr.courseTeacherAndRoomTextSizeScale
    private val tableHeight: Float
        get() = tableHeightBase * attr.tableHeightScale


    // 数据
    private var courseViews: ArrayList<View> = ArrayList()
    private var xAxisViews: ArrayList<View> = ArrayList()
    private var yAxisViews: ArrayList<View> = ArrayList()
    private var rows: Int = 0
    private var columns: Int = 0
    private var preCells: List<Cell> = emptyList()
    private var curCells: List<Cell> = emptyList()
    private var nextCells: List<Cell> = emptyList()
    private var preXAxis: List<XItem> = emptyList()
    private var curXAxis: List<XItem> = emptyList()
    private var nextXAxis: List<XItem> = emptyList()
    private var yAxis: List<YItem> = emptyList()
    private var highlightXIds: List<Long> = emptyList()
    private var highlightYIds: List<Long> = emptyList()

    fun updateData(
        rows: Int,
        columns: Int,
        preCells: List<Cell> = emptyList(),
        curCells: List<Cell> = emptyList(),
        nextCells: List<Cell> = emptyList(),
        preXAxis: List<XItem> = emptyList(),
        curXAxis: List<XItem> = emptyList(),
        nextXAxis: List<XItem> = emptyList(),
        yAxis: List<YItem> = emptyList(),
        highlightXIds: List<Long> = emptyList(),
        highlightYIds: List<Long> = emptyList(),
        attr: TableAttr = TableAttr.latest
    ) {
        if (rows <= 0 || columns <= 0)
            throw IllegalArgumentException("rows and columns must be positive")
        this.rows = rows
        this.columns = columns
        this.preCells = preCells
        this.curCells = curCells
        this.nextCells = nextCells
        this.preXAxis = preXAxis
        this.curXAxis = curXAxis
        this.nextXAxis = nextXAxis
        this.yAxis = yAxis
        this.highlightXIds = highlightXIds
        this.highlightYIds = highlightYIds
        this.attr = attr
        val dealViews = { list: ArrayList<View>, count: Int, id: Int ->
            while (list.size < count) {
                LayoutInflater.from(context)
                    .inflate(id, this, false)
                    .also {
                        list.add(it)
                        addView(it)
                    }
            }
            for (i in 0 until count) {
                list[i].isGone = false
                list[i].translationX = 0f
            }
            for (i in count until list.size) {
                list[i].isGone = true
            }
        }
        dealViews(
            courseViews,
            preCells.size + curCells.size + nextCells.size,
            R.layout.item_table_cell
        )
        dealViews(
            xAxisViews,
            columns * 3,
            R.layout.item_table_x
        )
        dealViews(
            yAxisViews,
            rows,
            R.layout.item_table_y
        )
        offsetX = 0f
        bindData()
        requestLayout()
        invalidate()
    }

    /* ==== 绑定数据 ==== */
    private fun bindData() {
        if (columns <= 0 || rows <= 0) return

        // course
        val bindCourse = { view: View, cell: Cell ->
            with(ItemTableCellBinding.bind(view)) {
                bg.isEnabled = true
                bg.setOnClickListenerWithClickAnimation { onClickCell(cell) }
                bgFill.backgroundTintList = ColorStateList.valueOf(cell.color)
                title.text = cell.title
                content.text = cell.content
                title.textSize = nameTextSize
                content.textSize = teacherAndRoomTextSize
            }
        }
        for (i in preCells.indices) {
            val cell = preCells[i]
            val view = courseViews[i]
            bindCourse(view, cell)
        }
        for (i in curCells.indices) {
            val cell = curCells[i]
            val view = courseViews[i + preCells.size]
            bindCourse(view, cell)
        }
        for (i in nextCells.indices) {
            val cell = nextCells[i]
            val view = courseViews[i + preCells.size + curCells.size]
            bindCourse(view, cell)
        }

        // 横坐标
        val bindXAxis = { view: View, item: XItem ->
            with(ItemTableXBinding.bind(view)) {
                bg.isEnabled = false
                dayOfWeek.text = item.dayOfWeek
                date.text = item.date
                dayOfWeek.textSize = dayOfWeekTextSize
                date.textSize = dateTextSize
                if (context.isNightModeYes()) {
                    bgFill.isVisible = item.id in highlightXIds
                } else {
                    if (item.id in highlightXIds) {
                        bgFill.isVisible = true
                        dayOfWeek.setTextColor(white)
                        date.setTextColor(white)
                    } else {
                        bgFill.isVisible = false
                        dayOfWeek.setTextColor(black)
                        date.setTextColor(black)
                    }
                }
            }
        }
        for (i in 0 until columns) {
            val view = xAxisViews[i]
            bindXAxis(view, preXAxis[i])
        }
        for (i in 0 until columns) {
            val view = xAxisViews[i + columns]
            bindXAxis(view, curXAxis[i])
        }
        for (i in 0 until columns) {
            val view = xAxisViews[i + columns * 2]
            bindXAxis(view, nextXAxis[i])
        }

        // 纵坐标
        for (i in 0 until rows) {
            val view = yAxisViews[i]
            val item = yAxis[i]
            with(ItemTableYBinding.bind(view)) {
                bg.isEnabled = false
                nodeNumber.text = item.nodeNumber
                time.text = item.time
                nodeNumber.textSize = nodeNumberTextSize
                time.textSize = timeTextSize
            }
        }
    }

    /* ==== 测量每个cell的尺寸 ==== */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
        if (columns <= 0 || rows <= 0) return
        val w = (width - yW) / columns
        val h = (height - xH) / rows
        val makeMeasureSpec = { size: Float, mode: Int ->
            MeasureSpec.makeMeasureSpec(size.toInt(), mode)
        }
        val measureCell = { cell: Cell, view: View ->
            val cw = w * (cell.column.second - cell.column.first + 1)
            val ch = h * (cell.row.second - cell.row.first + 1)
            measureChild(
                view,
                makeMeasureSpec(cw, MeasureSpec.EXACTLY),
                makeMeasureSpec(ch, MeasureSpec.EXACTLY)
            )
        }
        // 顺序 pre - cur - next
        for (i in preCells.indices) {
            val child = courseViews[i]
            val cell = preCells[i]
            measureCell(cell, child)
        }
        for (i in curCells.indices) {
            val child = courseViews[i + preCells.size]
            val cell = curCells[i]
            measureCell(cell, child)
        }
        for (i in nextCells.indices) {
            val child = courseViews[i + preCells.size + curCells.size]
            val cell = nextCells[i]
            measureCell(cell, child)
        }
        // 横坐标
        for (i in 0 until columns * 3) {
            val child = xAxisViews[i]
            measureChild(
                child,
                makeMeasureSpec(w, MeasureSpec.EXACTLY),
                makeMeasureSpec(xH, MeasureSpec.EXACTLY)
            )
        }
        for (i in 0 until rows) {
            val child = yAxisViews[i]
            measureChild(
                child,
                makeMeasureSpec(yW, MeasureSpec.EXACTLY),
                makeMeasureSpec(h, MeasureSpec.EXACTLY)
            )
        }
    }

    // 颜色相关
    private val white = context.getColor(R.color.white)
    private val black = context.getColor(R.color.black)
    private fun View.layout(l: Float, t: Float, r: Float, b: Float) {
        layout(l.toInt(), t.toInt(), r.toInt(), b.toInt())
    }

    /* ==== 布局每个cell ==== */
    @SuppressLint("SetTextI18n")
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (columns <= 0 || rows <= 0) return
        val w = (r - l - yW) / columns
        val h = (b - t - xH) / rows

        // course
        for (i in preCells.indices) {
            val cell = preCells[i]
            val view = courseViews[i]
            view.layout(
                (cell.column.first - 1) * w + yW - width,
                (cell.row.first - 1) * h + xH,
                (cell.column.second) * w + yW - width,
                (cell.row.second) * h + xH
            )
        }
        for (i in curCells.indices) {
            val cell = curCells[i]
            val view = courseViews[i + preCells.size]
            view.layout(
                (cell.column.first - 1) * w + yW,
                (cell.row.first - 1) * h + xH,
                (cell.column.second) * w + yW,
                (cell.row.second) * h + xH
            )
        }
        for (i in nextCells.indices) {
            val cell = nextCells[i]
            val view = courseViews[i + preCells.size + curCells.size]
            view.layout(
                (cell.column.first - 1) * w + yW + width,
                (cell.row.first - 1) * h + xH,
                (cell.column.second) * w + yW + width,
                (cell.row.second) * h + xH
            )
        }

        // 横坐标
        for (i in 0 until columns) {
            val view = xAxisViews[i]
            view.layout(
                i * w + yW - width,
                0f,
                (i + 1) * w + yW - width,
                xH
            )
        }
        for (i in 0 until columns) {
            val view = xAxisViews[i + columns]
            view.layout(
                i * w + yW,
                0f,
                (i + 1) * w + yW,
                xH
            )
        }
        for (i in 0 until columns) {
            val view = xAxisViews[i + columns * 2]
            view.layout(
                i * w + yW + width,
                0f,
                (i + 1) * w + yW + width,
                xH
            )
        }

        // 纵坐标
        for (i in 0 until rows) {
            val view = yAxisViews[i]
            view.layout(
                0f,
                i * h + xH,
                yW,
                (i + 1) * h + xH
            )
        }
    }

    // 滚动相关
    private val scroller = OverScroller(context)
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop
    private val minFling = ViewConfiguration.get(context).scaledMinimumFlingVelocity
    private val maxFling = ViewConfiguration.get(context).scaledMaximumFlingVelocity
    private val thresholdsScaleWithWidth = 0.7f
    private var downX = 0f
    private var lastX = 0f
    private var offsetX = 0f
    private var velocityTracker: VelocityTracker? = null
    private var activePointerId = MotionEvent.INVALID_POINTER_ID
    private var scrollAnimator: ValueAnimator? = null
    var onScrollListener: (offsetX: Float, width: Float) -> Unit = { _, _ -> }

    /* ==== 事件拦截 ==== */
    override fun onInterceptTouchEvent(ev: MotionEvent?): Boolean {
        ev ?: return false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = ev.x
                activePointerId = ev.getPointerId(0)
                if (!scroller.isFinished) {
                    scroller.abortAnimation()
                }
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = ev.x - downX
                lastX = ev.x
                if (abs(dx) > touchSlop) return true
            }

            else -> activePointerId = MotionEvent.INVALID_POINTER_ID
        }
        return false
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return false
        if (activePointerId == MotionEvent.INVALID_POINTER_ID) return false
        velocityTracker = velocityTracker ?: VelocityTracker.obtain()
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                lastX = event.x
                offsetX += dx
                offsetX = offsetX.coerceIn(-width.toFloat(), width.toFloat())
                scrollAnimator?.apply {
                    cancel()
                    scrollAnimator = null
                }
                val cellCount = preCells.size + curCells.size + nextCells.size
                for (i in 0 until cellCount) {
                    courseViews[i].translationX = offsetX
                }
                for (i in 0 until columns * 3) {
                    xAxisViews[i].translationX = offsetX
                }
                onScrollListener(offsetX, width.toFloat())
                invalidate()
            }

            MotionEvent.ACTION_UP -> {
                velocityTracker?.apply {
                    computeCurrentVelocity(1000, maxFling.toFloat())
                    val xVel = getXVelocity(activePointerId)
                    if (abs(xVel) >= minFling) fling(xVel)
                    else absorbPage()
                    recycle()
                    velocityTracker = null
                }
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }

            else -> {
                activePointerId = MotionEvent.INVALID_POINTER_ID
            }
        }
        velocityTracker?.addMovement(event)
        return true
    }

    private fun fling(velocityX: Float) {
        val (minX, maxX) = -width to width
        scroller.fling(
            offsetX.toInt(), 0, velocityX.toInt(), 0,
            minX, maxX, 0, 0, 0, 0
        )
        invalidate()
    }

    override fun computeScroll() {
        onScrollListener(offsetX, width.toFloat())
        if (!scroller.isFinished && scroller.computeScrollOffset()) {
            offsetX = scroller.currX.toFloat()
            val curVel = scroller.currVelocity
            if (abs(curVel) < minFling ||
                abs(offsetX) > width * thresholdsScaleWithWidth
            ) {
                scroller.abortAnimation()
                absorbPage()
            } else {
                val cellCount = preCells.size + curCells.size + nextCells.size
                for (i in 0 until cellCount) {
                    courseViews[i].translationX = offsetX
                }
                for (i in 0 until columns * 3) {
                    xAxisViews[i].translationX = offsetX
                }
                invalidate()
            }
        }
    }

    /* ==== 吸附归位 ==== */
    @SuppressLint("Recycle")
    private fun absorbPage() {
        val thresholds = width * thresholdsScaleWithWidth
        SCLog.debug("absorbPage", "offsetX=$offsetX")
        SCLog.debug("absorbPage", "thresholds=$thresholds")
        val targetX = when (offsetX) {
            in thresholds..width.toFloat() -> width
            in -width.toFloat()..-thresholds -> -width
            else -> 0
        }.toFloat()
        val cellCount = preCells.size + curCells.size + nextCells.size
        scrollAnimator = ValueAnimator.ofFloat(offsetX, targetX).apply {
            duration = 200L // 200ms 在手机竖屏上操控的手感最好
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Float
                offsetX = value
                for (i in 0 until cellCount) {
                    courseViews[i].translationX = value
                }
                for (i in 0 until columns * 3) {
                    xAxisViews[i].translationX = value
                }
            }
            doOnEnd {
                scrollAnimator = null
                when (offsetX.toInt()) {
                    width -> onScrollToPre(preCells)
                    -width -> onScrollToNext(nextCells)
                    else -> Unit
                }
            }
            start()
        }
    }

    data class Cell(
        val id: Long,
        val title: String,
        val content: String,
        val color: Int = 0,
        val row: Pair<Int, Int>,
        val column: Pair<Int, Int>
    )

    data class XItem(
        val id: Long,
        val dayOfWeek: String,
        val date: String,
    )

    data class YItem(
        val id: Long,
        val nodeNumber: String,
        val time: String,
    )
}