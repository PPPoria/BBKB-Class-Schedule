package com.bbkb.sc.ui.widget

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.util.AttributeSet
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.graphics.toColorInt
import com.bbkb.sc.databinding.ItemTableBinding
import androidx.core.view.isGone
import com.bbkb.sc.R
import com.poria.base.ext.dp2px
import com.poria.base.ext.setOnClickListenerWithClickAnimation



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

    private val white = context.getColor(R.color.white)
    private val whiteStateList = ColorStateList.valueOf(white)
    private val black = context.getColor(R.color.black)
    private val highlightColor = context.getColor(R.color.secondary)

    private val yW = dp2px(35f)
    private val xH = dp2px(41f)
    private var rows: Int = 0
    private var columns: Int = 0
    private var cells: List<Cell> = emptyList()
    private var itemViews: ArrayList<View> = ArrayList()
    private var listener: (Cell) -> Unit = {}
    private var xAxis: List<String> = emptyList()
    private var yAxis: List<String> = emptyList()
    private var highlightX: Int = 0
    private var highlightY: Int = 0

    fun update(
        rows: Int,
        columns: Int,
        cells: List<Cell>,
        xAxis: List<String> = emptyList(),
        yAxis: List<String> = emptyList(),
        highlightX: Int = 0,
        highlightY: Int = 0,
        listener: (Cell) -> Unit = {}
    ) {
        if (rows <= 0 || columns <= 0)
            throw IllegalArgumentException("rows and columns must be positive")
        this.rows = rows
        this.columns = columns
        this.cells = cells
        this.listener = listener
        this.xAxis = xAxis
        this.yAxis = yAxis
        this.highlightX = highlightX
        this.highlightY = highlightY
        val sum = rows + columns + cells.size + 1 // 多一个原点的View
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
        }
        for (i in sum until itemViews.size) {
            itemViews[i].isGone = true
        }
        requestLayout()
    }

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
        for (i in cells.indices) {
            val child = getChildAt(i)
            val cell = cells[i]
            val cw = w * (cell.column.second - cell.column.first + 1)
            val ch = h * (cell.row.second - cell.row.first + 1)
            measureChild(
                child,
                makeMeasureSpec(cw, MeasureSpec.EXACTLY),
                makeMeasureSpec(ch, MeasureSpec.EXACTLY)
            )
        }
        for (i in 0 until columns) {
            val child = getChildAt(i + cells.size)
            measureChild(
                child,
                makeMeasureSpec(w, MeasureSpec.EXACTLY),
                makeMeasureSpec(xH * 2, MeasureSpec.EXACTLY)
            )
        }
        for (i in 0 until rows) {
            val child = getChildAt(i + cells.size + columns)
            measureChild(
                child,
                makeMeasureSpec(yW, MeasureSpec.EXACTLY),
                makeMeasureSpec(h * 2, MeasureSpec.EXACTLY)
            )
        }
        measureChild(
            getChildAt(cells.size + columns + rows),
            makeMeasureSpec(yW, MeasureSpec.EXACTLY),
            makeMeasureSpec(xH, MeasureSpec.EXACTLY)
        )
    }

    private fun View.layout(l: Float, t: Float, r: Float, b: Float) {
        layout(l.toInt(), t.toInt(), r.toInt(), b.toInt())
    }

    @SuppressLint("SetTextI18n")
    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (columns <= 0 || rows <= 0) return
        val w = (r - l - yW) / columns
        val h = (b - t - xH) / rows
        // 绘制表格
        for (i in cells.indices) {
            val cell = cells[i]
            val view = itemViews[i]
            with(ItemTableBinding.bind(view)) {
                container.backgroundTintList = ColorStateList
                    .valueOf("#00000000".toColorInt())
                bg.isEnabled = true
                bg.setOnClickListenerWithClickAnimation { listener(cell) }
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
            view.layout(
                (cell.column.first - 1) * w + yW,
                (cell.row.first - 1) * h + xH,
                (cell.column.second) * w + yW,
                (cell.row.second) * h + xH
            )
        }
        // 绘制横坐标
        for (i in 0 until columns) {
            val view = itemViews[i + cells.size]
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
        // 绘制纵坐标
        for (i in 0 until rows) {
            val view = itemViews[i + cells.size + columns]
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
        with(ItemTableBinding.bind(itemViews[cells.size + columns + rows])) {
            container.backgroundTintList = whiteStateList
            bg.isEnabled = false
            bg.backgroundTintList = whiteStateList
            title.text = ""
            content.text = ""
            root.layout(0f, 0f, yW, xH)
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