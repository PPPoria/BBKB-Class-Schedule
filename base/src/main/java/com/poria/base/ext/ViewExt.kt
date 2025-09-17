package com.poria.base.ext

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View

@SuppressLint("ClickableViewAccessibility")
fun View.setOnClickListenerWithClickAnimation(listener: (View) -> Unit) {
    val duration = 50L
    setOnTouchListener { v, event ->
        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                v.clearAnimation()
                v.animate()
                    .setDuration(duration)
                    .scaleX(0.95f)
                    .scaleY(0.95f)
                    .alpha(0.95f)
                    .start()
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                v.clearAnimation()
                v.animate()
                    .setDuration(duration)
                    .scaleX(1f)
                    .scaleY(1f)
                    .alpha(1f)
                    .start()
            }
        }
        false
    }
    setOnClickListener(listener)
}