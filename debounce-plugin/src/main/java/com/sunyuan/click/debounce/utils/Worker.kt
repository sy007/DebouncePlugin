package com.sunyuan.click.debounce.utils

import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author sy007
 * @date 2022/09/03
 * @description
 */
class Worker {
    private val cpuCount = Runtime.getRuntime().availableProcessors()
    private val executor = ThreadPoolExecutor(
        cpuCount, cpuCount,
        0L, TimeUnit.MILLISECONDS,
        LinkedBlockingQueue(), object : ThreadFactory {
            private var threadNumber = AtomicInteger(1)
            override fun newThread(r: Runnable): Thread {
                return Thread(r).apply {
                    name = "debounce-transform-thread-" + threadNumber.getAndIncrement()
                    isDaemon = true
                    priority = Thread.NORM_PRIORITY
                }
            }
        }
    )

    fun submit(task: () -> Unit): Future<*> {
        return executor.submit(task)
    }

    fun shutdown() {
        executor.shutdown()
    }

    fun awaitTermination(timeout: Long, unit: TimeUnit) {
        executor.awaitTermination(timeout, unit)
    }
}