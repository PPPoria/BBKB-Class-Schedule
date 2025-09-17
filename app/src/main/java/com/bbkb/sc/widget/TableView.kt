package com.bbkb.sc.widget

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
import com.bbkb.sc.util.SCLog
import com.poria.base.ext.setOnClickListenerWithClickAnimation

private const val TAG = "TableView"

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

    private var rows: Int = 0
    private var columns: Int = 0
    private var count: Int = 0
    private var cells: List<Cell> = emptyList()
    private var itemViews: ArrayList<View> = ArrayList()
    private var listener: (Cell) -> Unit = {}
    private var xAxis: List<String> = emptyList()
    private var yAxis: List<String> = emptyList()

    fun update(
        rows: Int,
        columns: Int,
        cells: List<Cell>,
        xAxis: List<String> = emptyList(),
        yAxis: List<String> = emptyList(),
        listener: (Cell) -> Unit = {}
    ) {
        if (rows <= 0 || columns <= 0)
            throw IllegalArgumentException("rows and columns must be positive")
        this.rows = rows
        this.columns = columns
        count = cells.size
        this.cells = cells
        this.listener = listener
        this.xAxis = xAxis
        this.yAxis = yAxis
        while (itemViews.size < count + columns) {
            LayoutInflater.from(context)
                .inflate(R.layout.item_table, this, false)
                .also {
                    itemViews.add(it)
                    addView(it)
                }
        }
        for (i in 0 until count + columns) {
            itemViews[i].isGone = false
        }
        for (i in count + columns until itemViews.size) {
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
        val w = width / columns
        val h = height / (rows + 1)

        // 让子 View 按“格子大小”走一遍 measure
        for (i in 0 until count) {
            val child = getChildAt(i)
            val cell = cells[i]
            val cw = w * (cell.column.second - cell.column.first + 1)
            val ch = h * (cell.row.second - cell.row.first + 1)
            measureChild(
                child,
                MeasureSpec.makeMeasureSpec(cw, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(ch, MeasureSpec.EXACTLY)
            )
        }
        for (i in count until count + columns) {
            val child = getChildAt(i)
            measureChild(
                child,
                MeasureSpec.makeMeasureSpec(w, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(h, MeasureSpec.EXACTLY)
            )
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        if (columns <= 0 || rows <= 0) return
        val w = (r - l) / columns
        val h = (b - t) / (rows + 1)
        for (i in 0..<count) {
            val cell = cells[i]
            val view = itemViews[i]
            ItemTableBinding.bind(view).apply {
                bg.isEnabled = true
                bg.setOnClickListenerWithClickAnimation {
                    listener(cell)
                }
                bg.backgroundTintList = ColorStateList
                    .valueOf(
                        if (cell.color == 0) "#FFFFFF".toColorInt()
                        else cell.color
                    )
                title.isGone = cell.title?.let {
                    title.text = it
                    false
                } ?: true
                content.text = cell.content
                content.gravity = Gravity.BOTTOM
            }
            view.layout(
                (cell.column.first - 1) * w,
                h / 2 + (cell.row.first - 1) * h,
                (cell.column.second) * w,
                h / 2 + (cell.row.second) * h
            )
        }
        for (i in count until count + columns) {
            val view = itemViews[i]
            ItemTableBinding.bind(view).apply {
                bg.isEnabled = false
                bg.backgroundTintList = ColorStateList
                    .valueOf("#ffffff".toColorInt())
                title.also {
                    it.isGone = false
                    it.text = ""
                }
                content.text = xAxis.let {
                    val index = i - count
                    if (index < it.size) it[index]
                    else (index + 1).toString()
                }
                content.gravity = Gravity.CENTER
            }
            view.layout(
                (i - count) * w,
                -h / 2,
                (i - count + 1) * w,
                h / 2
            )
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