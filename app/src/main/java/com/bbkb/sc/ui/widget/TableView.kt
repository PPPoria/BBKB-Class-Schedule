package com.bbkb.sc.ui.widget

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.OverScroller
import androidx.core.animation.doOnEnd
import androidx.core.graphics.toColorInt
import com.bbkb.sc.databinding.ItemTableBinding
import androidx.core.view.isGone
import com.bbkb.sc.R
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

    // 尺寸、位置相关
    private val yW = dp2px(35f)
    private val xH = dp2px(41f)
    private var rows: Int = 0
    private var columns: Int = 0
    private var preCells: List<Cell> = emptyList()
    private var curCells: List<Cell> = emptyList()
    private var nextCells: List<Cell> = emptyList()
    private var itemViews: ArrayList<View> = ArrayList()
    private var onClickCell: (Cell) -> Unit = {}
    private var onScrollToPre: (List<Cell>) -> Unit = {}
    private var onScrollToNext: (List<Cell>) -> Unit = {}
    private var xAxis: List<String> = emptyList()
    private var yAxis: List<String> = emptyList()
    private var highlightX: Int = 0
    private var highlightY: Int = 0

    fun update(
        rows: Int,
        columns: Int,
        preCells: List<Cell> = emptyList(),
        curCells: List<Cell> = emptyList(),
        nextCells: List<Cell> = emptyList(),
        xAxis: List<String> = emptyList(),
        yAxis: List<String> = emptyList(),
        highlightX: Int = 0,
        highlightY: Int = 0,
        onClickCell: (Cell) -> Unit = {},
        onScrollToPre: (List<Cell>) -> Unit = {},
        onScrollToNext: (List<Cell>) -> Unit = {}
    ) {
        if (rows <= 0 || columns <= 0)
            throw IllegalArgumentException("rows and columns must be positive")
        this.rows = rows
        this.columns = columns
        this.preCells = preCells
        this.curCells = curCells
        this.nextCells = nextCells
        this.onClickCell = onClickCell
        this.onScrollToPre = onScrollToPre
        this.onScrollToNext = onScrollToNext
        this.xAxis = xAxis
        this.yAxis = yAxis
        this.highlightX = highlightX
        this.highlightY = highlightY
        val sum = (rows + columns + 1 + // 多一个原点的View
                preCells.size + curCells.size + nextCells.size)
        while (itemViews.size < sum) {
            LayoutInflater.from(context)
                .inflate(R.layout.item_table, this, false)
                .also {
                    itemViews.add(it)
                    addView(it)
                }
        }
        for (i in 0 until sum) {
            itemViews[i].isGone = false
            itemViews[i].translationX = 0f
        }
        for (i in sum until itemViews.size) {
            itemViews[i].isGone = true
        }
        offsetX = 0f
        requestLayout()
        invalidate()
    }

    /* ==== 测量每个cell的尺寸 ==== */
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 1. 父容器先给自己定尺寸
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)

        // 2. 把子 View 当成 MATCH_PARENT 测一次（以格子大小为约束）
        if (columns <= 0 || rows <= 0) return
        val w = (width - yW) / columns
        val h = (height - xH) / rows
        val makeMeasureSpec = { size: Float, mode: Int ->
            MeasureSpec.makeMeasureSpec(size.toInt(), mode)
        }

        // 让子 View 按“格子大小”走一遍 measure
        var indexOffset = 0
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
            val child = getChildAt(i)
            val cell = preCells[i]
            measureCell(cell, child)
        }
        indexOffset += preCells.size
        for (i in curCells.indices) {
            val child = getChildAt(i + indexOffset)
            val cell = curCells[i]
            measureCell(cell, child)
        }
        indexOffset += curCells.size
        for (i in nextCells.indices) {
            val child = getChildAt(i + indexOffset)
            val cell = nextCells[i]
            measureCell(cell, child)
        }
        indexOffset += nextCells.size
        // 横坐标
        for (i in 0 until columns) {
            val child = getChildAt(i + indexOffset)
            measureChild(
                child,
                makeMeasureSpec(w, MeasureSpec.EXACTLY),
                makeMeasureSpec(xH * 2, MeasureSpec.EXACTLY)
            )
        }
        indexOffset += columns
        for (i in 0 until rows) {
            val child = getChildAt(i + indexOffset)
            measureChild(
                child,
                makeMeasureSpec(yW, MeasureSpec.EXACTLY),
                makeMeasureSpec(h * 2, MeasureSpec.EXACTLY)
            )
        }
        indexOffset += rows
        measureChild(
            getChildAt(indexOffset),
            makeMeasureSpec(yW, MeasureSpec.EXACTLY),
            makeMeasureSpec(xH, MeasureSpec.EXACTLY)
        )
    }

    // 颜色相关
    private val white = context.getColor(R.color.white)
    private val whiteStateList = ColorStateList.valueOf(white)
    private val black = context.getColor(R.color.black)
    private val highlightColor = context.getColor(R.color.primary)

    private fun View.layout(l: Float, t: Float, r: Float, b: Float) {
        layout(l.toInt(), t.toInt(), r.toInt(), b.toInt())
    }

    /* ==== 布局每个cell ==== */
    @SuppressLint("SetTextI18n")
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (columns <= 0 || rows <= 0) return
        val w = (r - l - yW) / columns
        val h = (b - t - xH) / rows
        // 绘制Cells
        val bindCell = { view: View, cell: Cell ->
            with(ItemTableBinding.bind(view)) {
                container.backgroundTintList = ColorStateList
                    .valueOf("#00000000".toColorInt())
                bg.isEnabled = true
                bg.setOnClickListenerWithClickAnimation { onClickCell(cell) }
                bg.backgroundTintList = ColorStateList.valueOf(
                    if (cell.color == 0) white
                    else cell.color
                )
                title.gravity = Gravity.TOP
                title.text = cell.title ?: ""
                title.setTextColor(black)
                content.gravity = Gravity.BOTTOM
                content.text = cell.content
                content.setTextColor(black)
            }
        }
        var indexOffset = 0
        for (i in preCells.indices) {
            val cell = preCells[i]
            val view = getChildAt(i)
            bindCell(view, cell)
            view.layout(
                (cell.column.first - 1) * w + yW - width,
                (cell.row.first - 1) * h + xH,
                (cell.column.second) * w + yW - width,
                (cell.row.second) * h + xH
            )
        }
        indexOffset += preCells.size
        for (i in curCells.indices) {
            val cell = curCells[i]
            val view = getChildAt(i + indexOffset)
            bindCell(view, cell)
            view.layout(
                (cell.column.first - 1) * w + yW,
                (cell.row.first - 1) * h + xH,
                (cell.column.second) * w + yW,
                (cell.row.second) * h + xH
            )
        }
        indexOffset += curCells.size
        for (i in nextCells.indices) {
            val cell = nextCells[i]
            val view = getChildAt(i + indexOffset)
            bindCell(view, cell)
            view.layout(
                (cell.column.first - 1) * w + yW + width,
                (cell.row.first - 1) * h + xH,
                (cell.column.second) * w + yW + width,
                (cell.row.second) * h + xH
            )
        }
        indexOffset += nextCells.size
        // 绘制横坐标
        for (i in 0 until columns) {
            val view = getChildAt(i + indexOffset)
            with(ItemTableBinding.bind(view)) {
                container.backgroundTintList = whiteStateList
                bg.isEnabled = false
                bg.backgroundTintList = whiteStateList
                title.gravity = Gravity.CENTER
                title.text = xAxis.let {
                    if (i < it.size) it[i]
                    else (i + 1).toString()
                }
                title.setTextColor(black)
                if (i + 1 == highlightX) {
                    title.setTextColor(highlightColor)
                }
            }
            view.layout(
                i * w + yW,
                -1f,
                (i + 1) * w + yW,
                xH
            )
        }
        indexOffset += columns
        // 绘制纵坐标
        for (i in 0 until rows) {
            val view = getChildAt(i + indexOffset)
            with(ItemTableBinding.bind(view)) {
                container.backgroundTintList = whiteStateList
                bg.isEnabled = false
                bg.backgroundTintList = whiteStateList
                title.gravity = Gravity.CENTER
                title.text = yAxis.let { list ->
                    if (i < list.size) list[i]
                    else (i + 1).toString()
                }
                title.setTextColor(black)
                if (i + 1 == highlightY) {
                    title.setTextColor(highlightColor)
                }
            }
            view.layout(
                0f,
                i * h + xH,
                yW,
                (i + 1) * h + xH
            )
        }
        indexOffset += rows
        // 绘制原点
        with(ItemTableBinding.bind(getChildAt(indexOffset))) {
            container.backgroundTintList = whiteStateList
            bg.isEnabled = false
            bg.backgroundTintList = whiteStateList
            title.text = ""
            content.text = ""
            root.layout(0f, 0f, yW, xH)
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
                    val view = itemViews[i]
                    view.translationX = offsetX
                }
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
        if (!scroller.isFinished && scroller.computeScrollOffset()) {
            val cellCount = preCells.size + curCells.size + nextCells.size
            offsetX = scroller.currX.toFloat()
            val curVel = scroller.currVelocity
            if (abs(curVel) < minFling ||
                abs(offsetX) > width * thresholdsScaleWithWidth
            ) {
                scroller.abortAnimation()
                absorbPage()
            } else {
                for (i in 0 until cellCount) {
                    itemViews[i].translationX = offsetX
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
            duration = 300L
            interpolator = DecelerateInterpolator()
            addUpdateListener {
                val value = it.animatedValue as Float
                offsetX = value
                for (i in 0 until cellCount) {
                    itemViews[i].translationX = value
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
        val id: Int,
        val title: String? = null,
        val content: String,
        val postscript: String? = null,
        val color: Int = 0,
        val row: Pair<Int, Int>,
        val column: Pair<Int, Int>
    )
}