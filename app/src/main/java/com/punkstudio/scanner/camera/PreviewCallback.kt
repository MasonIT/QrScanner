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
internal class PreviewCallback(private val mConfigManager: CameraConfigurationManager) :
    Camera.PreviewCallback {
    private var mPreviewHandler: Handler? = null
    private var mPreviewMessage = 0
    fun setHandler(previewHandler: Handler?, previewMessage: Int) {
        mPreviewHandler = previewHandler
        mPreviewMessage = previewMessage
    }

    override fun onPreviewFrame(
        data: ByteArray,
        camera: Camera
    ) {
        val cameraResolution = mConfigManager.cameraResolution
        if (mPreviewHandler != null) {
            val message = mPreviewHandler!!.obtainMessage(
                mPreviewMessage,
                cameraResolution!!.width,
                cameraResolution.height,
                data
            )
            message.sendToTarget()
            mPreviewHandler = null
        } else {
            Log.v(
                TAG,
                "no handler callback."
            )
        }
    }

    companion object {
        private val TAG =
            PreviewCallback::class.java.name
    }

}