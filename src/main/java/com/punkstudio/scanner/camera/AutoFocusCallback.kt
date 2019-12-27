@file:Suppress("DEPRECATION")

package com.punkstudio.scanner.camera

import android.hardware.Camera
import android.os.Handler
import android.util.Log

/**
 * @author Mason
 *
 * @date 2019-03-05
 */
@Suppress("DEPRECATION")
internal class AutoFocusCallback : Camera.AutoFocusCallback {
    private var mAutoFocusHandler: Handler? = null
    private var mAutoFocusMessage = 0
    fun setHandler(autoFocusHandler: Handler?, autoFocusMessage: Int) {
        mAutoFocusHandler = autoFocusHandler
        mAutoFocusMessage = autoFocusMessage
    }

    override fun onAutoFocus(
        success: Boolean,
        camera: Camera
    ) {
        if (mAutoFocusHandler != null) {
            val message =
                mAutoFocusHandler!!.obtainMessage(mAutoFocusMessage, success)
            mAutoFocusHandler!!.sendMessageDelayed(
                message,
                AUTO_FOCUS_INTERVAL_MS
            )
            mAutoFocusHandler = null
        } else {
            Log.d(
                TAG,
                "Got auto-focus callback, but no handler for it"
            )
        }
    }

    companion object {
        private val TAG = AutoFocusCallback::class.java.name
        private const val AUTO_FOCUS_INTERVAL_MS = 1500L
    }
}