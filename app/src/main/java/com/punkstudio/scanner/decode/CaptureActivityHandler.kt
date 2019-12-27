package com.punkstudio.scanner.decode

import android.os.Handler
import android.os.Message
import android.util.Log
import com.google.zxing.Result
import com.punkstudio.scanner.ScannerActivity
import com.punkstudio.scanner.R
import com.punkstudio.scanner.camera.CameraManager.Companion.get

/**
 * @author Mason
 *
 * @date 2019-03-05
 */
class CaptureActivityHandler(private val mActivity: ScannerActivity) : Handler() {
    private val mDecodeThread: DecodeThread = DecodeThread(mActivity)
    private var mState: State

    private enum class State {
        PREVIEW, SUCCESS, DONE
    }

    override fun handleMessage(message: Message) {
        if (message.what == R.id.auto_focus) {
            Log.d(TAG, "Got auto-focus message")
            if (mState == State.PREVIEW) {
                get()?.requestAutoFocus(this, R.id.auto_focus)
            }
        } else if (message.what == R.id.decode_succeeded) {
            Log.e(TAG, "Got decode succeeded message")
            mState = State.SUCCESS
            mActivity.handleDecode(message.obj as Result)
        } else if (message.what == R.id.decode_failed) { // We're decoding as fast as possible, so when one decode fails, start another.
            mState = State.PREVIEW
            get()!!.requestPreviewFrame(
                mDecodeThread.handler,
                R.id.decode
            )
        }
    }

    fun quitSynchronously() {
        mState = State.DONE
        get()!!.stopPreview()
        val quit = Message.obtain(
            mDecodeThread.handler,
            R.id.quit
        )
        quit.sendToTarget()
        try {
            mDecodeThread.join()
        } catch (e: InterruptedException) { // continue
        }
        // Be absolutely sure we don't send any queued up messages
        removeMessages(R.id.decode_succeeded)
        removeMessages(R.id.decode_failed)
    }

    fun restartPreviewAndDecode() {
        if (mState != State.PREVIEW) {
            get()!!.startPreview()
            mState = State.PREVIEW
            get()!!.requestPreviewFrame(
                mDecodeThread.handler,
                R.id.decode
            )
            get()?.requestAutoFocus(this, R.id.auto_focus)
        }
    }

    companion object {
        private val TAG = CaptureActivityHandler::class.java.name
    }

    init {
        mDecodeThread.start()
        mState = State.SUCCESS
        // Start ourselves capturing previews and decoding.
        restartPreviewAndDecode()
    }
}