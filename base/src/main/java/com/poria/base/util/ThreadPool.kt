package com.poria.base.util

import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.ThreadPoolExecutor

object ThreadPool {
    private val threadPool = ThreadPoolExecutor(
        Runtime.getRuntime().availableProcessors(),
        Runtime.getRuntime().availableProcessors(),
        200L,
        java.util.concurrent.TimeUnit.MILLISECONDS,
        LinkedBlockingDeque(),
    )

    fun execute(runnable: Runnable) {
        threadPool.execute(runnable)
    }
}