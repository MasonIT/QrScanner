package com.punkstudio.scanner.decode

import android.os.Handler
import android.os.Looper
import com.punkstudio.scanner.ScannerActivity
import java.util.concurrent.CountDownLatch

/**
 * @author Mason
 *
 * @date 2019-03-05
 */
internal class DecodeThread(private val mActivity: ScannerActivity) : Thread() {
    private var mHandler: Handler? = null
    private val mHandlerInitLatch: CountDownLatch
    // continue?
    val handler: Handler?
        get() {
            try {
                mHandlerInitLatch.await()
            } catch (ie: InterruptedException) { // continue?
            }
            return mHandler
        }

    override fun run() {
        Looper.prepare()
        mHandler = DecodeHandler(mActivity)
        mHandlerInitLatch.countDown()
        Looper.loop()
    }

    init {
        mHandlerInitLatch = CountDownLatch(1)
    }
}