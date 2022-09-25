package com.sunyuan.click.debounce.utils

import com.didiglobal.booster.kotlinx.NCPU
import java.io.IOException
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger

/**
 * @author sy007
 * @date 2022/09/03
 * @description
 */
class Worker {

    private val transformExecutor = ThreadPoolExecutor(
        NCPU, NCPU,
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
        }) { r, _ -> r.run() }


    fun submit(task: () -> Unit) {
        try {
            transformExecutor.submit(task).get()
        } catch (e: ExecutionException) {
            e.throwRealException()
        } catch (e: InterruptedException) {
            e.throwRealException()
        }
    }

    private fun Exception.throwRealException() {
        when (cause) {
            is IOException -> {
                throw cause as IOException
            }
            is RuntimeException -> {
                throw cause as RuntimeException
            }
            is Error -> {
                throw cause as Error
            }
            else -> throw  RuntimeException(cause)
        }
    }

    fun shutdown() {
        transformExecutor.shutdown()
    }

    fun awaitTermination(timeout: Long, unit: TimeUnit) {
        transformExecutor.awaitTermination(timeout, unit)
    }
}