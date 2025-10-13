package com.bbkb.sc.ui.widget

import android.content.Context
import android.graphics.Outline
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import androidx.constraintlayout.widget.ConstraintLayout
import com.bbkb.sc.R

class RoundConstraintLayout : ConstraintLayout {
    constructor(context: Context) : super(context)
    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        init(context, attrs)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        defStyleRes: Int
    ) : super(context, attrs, defStyleAttr, defStyleRes) {
        init(context, attrs)
    }

    private var mCornerRadius = 0f
    val cornerRadius: Float
        get() = mCornerRadius

    private fun init(
        context: Context,
        attrs: AttributeSet?,
    ) {
        context.obtainStyledAttributes(attrs, R.styleable.RoundConstraintLayout).apply {
            mCornerRadius = getDimension(
                R.styleable.RoundConstraintLayout_cornerRadius,
                0f
            )
            recycle()
        }
        // 关键：开启硬件裁剪
        outlineProvider = RoundOutlineProvider(mCornerRadius)
        clipToOutline = true
    }

    private class RoundOutlineProvider(private val radius: Float) : ViewOutlineProvider() {
        override fun getOutline(view: View, outline: Outline) {
            outline.setRoundRect(0, 0, view.width, view.height, radius)
        }
    }
}