package com.punkstudio.scanner.decode

import android.app.Activity
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ThreadFactory
import java.util.concurrent.TimeUnit

/**
 * @author Mason
 *
 * @date 2019-03-05
 */
class InactivityTimer(private val activity: Activity) {
    private val inactivityTimer = Executors.newSingleThreadScheduledExecutor(DaemonThreadFactory())
    private var inactivityFuture: ScheduledFuture<*>? = null
    fun onActivity() {
        cancel()
        inactivityFuture = inactivityTimer.schedule(
            FinishListener(activity),
            INACTIVITY_DELAY_SECONDS.toLong(),
            TimeUnit.SECONDS
        )
    }

    private fun cancel() {
        if (inactivityFuture != null) {
            inactivityFuture!!.cancel(true)
            inactivityFuture = null
        }
    }

    fun shutdown() {
        cancel()
        inactivityTimer.shutdown()
    }

    private class DaemonThreadFactory : ThreadFactory {
        override fun newThread(runnable: Runnable): Thread {
            val thread = Thread(runnable)
            thread.isDaemon = true
            return thread
        }
    }

    companion object {
        private const val INACTIVITY_DELAY_SECONDS = 5 * 60
    }

    init {
        onActivity()
    }
}