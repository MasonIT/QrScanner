package com.punkstudio.scanner.camera

import android.content.Context
import android.hardware.Camera
import android.os.Handler
import android.view.SurfaceHolder
import java.io.IOException

/**
 * @author Mason
 *
 * @date 2019-03-05
 */
class CameraManager private constructor(context: Context) {
    private val mConfigManager: CameraConfigurationManager = CameraConfigurationManager(context)
    private var mCamera: Camera? = null
    private var mInitialized = false
    private var mPreviewing = false
    /**
     * Preview frames are delivered here, which we pass on to the registered handler. Make sure to clear the handler so
     * it will only receive one message.
     */
    private val mPreviewCallback: PreviewCallback
    /** Auto-focus callbacks arrive here, and are dispatched to the Handler which requested them.  */
    private val mAutoFocusCallback: AutoFocusCallback

    /**
     * Opens the mCamera driver and initializes the hardware parameters.
     *
     * @param holder The surface object which the mCamera will draw preview frames into.
     * @throws IOException Indicates the mCamera driver failed to open.
     */
    @Throws(IOException::class)
    fun openDriver(holder: SurfaceHolder?) {
        if (mCamera == null) {
            mCamera = Camera.open()
            if (mCamera == null) {
                throw IOException()
            }
            mCamera!!.setPreviewDisplay(holder)
            if (!mInitialized) {
                mInitialized = true
                mConfigManager.initFromCameraParameters(mCamera!!)
            }
            mConfigManager.setDesiredCameraParameters(mCamera!!)
        }
    }

    fun setFlashLight(open: Boolean): Boolean {
        if (mCamera == null) {
            return false
        }
        val parameters = mCamera!!.parameters ?: return false
        val flashModes =
            parameters.supportedFlashModes
        // Check if camera flash exists
        if (null == flashModes || 0 == flashModes.size) { // Use the screen as a flashlight (next best thing)
            return false
        }
        val flashMode = parameters.flashMode
        return if (open) {
            if (Camera.Parameters.FLASH_MODE_TORCH == flashMode) {
                return true
            }
            // Turn on the flash
            if (flashModes.contains(Camera.Parameters.FLASH_MODE_TORCH)) {
                parameters.flashMode = Camera.Parameters.FLASH_MODE_TORCH
                mCamera!!.parameters = parameters
                true
            } else {
                false
            }
        } else {
            if (Camera.Parameters.FLASH_MODE_OFF == flashMode) {
                return true
            }
            // Turn on the flash
            if (flashModes.contains(Camera.Parameters.FLASH_MODE_OFF)) {
                parameters.flashMode = Camera.Parameters.FLASH_MODE_OFF
                mCamera!!.parameters = parameters
                true
            } else false
        }
    }

    /**
     * Closes the camera driver if still in use.
     */
    fun closeDriver() {
        if (mCamera != null) {
            mCamera!!.release()
            mInitialized = false
            mPreviewing = false
            mCamera = null
        }
    }

    /**
     * Asks the mCamera hardware to begin drawing preview frames to the screen.
     */
    fun startPreview() {
        if (mCamera != null && !mPreviewing) {
            mCamera!!.startPreview()
            mPreviewing = true
        }
    }

    /**
     * Tells the mCamera to stop drawing preview frames.
     */
    fun stopPreview() {
        if (mCamera != null && mPreviewing) {
            mCamera!!.stopPreview()
            mPreviewCallback.setHandler(null, 0)
            mAutoFocusCallback.setHandler(null, 0)
            mPreviewing = false
        }
    }

    /**
     * A single preview frame will be returned to the handler supplied. The data will arrive as byte[] in the
     * message.obj field, with width and height encoded as message.arg1 and message.arg2, respectively.
     *
     * @param handler The handler to send the message to.
     * @param message The what field of the message to be sent.
     */
    fun requestPreviewFrame(handler: Handler?, message: Int) {
        if (mCamera != null && mPreviewing) {
            mPreviewCallback.setHandler(handler, message)
            mCamera!!.setOneShotPreviewCallback(mPreviewCallback)
        }
    }

    /**
     * Asks the mCamera hardware to perform an autofocus.
     *
     * @param handler The Handler to notify when the autofocus completes.
     * @param message The message to deliver.
     */
    fun requestAutoFocus(handler: Handler?, message: Int) {
        if (mCamera != null && mPreviewing) {
            mAutoFocusCallback.setHandler(handler, message)
            // Log.d(TAG, "Requesting auto-focus callback");
            mCamera!!.autoFocus(mAutoFocusCallback)
        }
    }

    companion object {
        private var sCameraManager: CameraManager? = null
        /**
         * Initializes this static object with the Context of the calling Activity.
         */
        @JvmStatic
        fun init(context: Context) {
            if (sCameraManager == null) {
                sCameraManager =
                    CameraManager(context)
            }
        }

        /**
         * Gets the CameraManager singleton instance.
         *
         * @return A reference to the CameraManager singleton.
         */
        @JvmStatic
        fun get(): CameraManager? {
            return sCameraManager
        }
    }

    init {
        mPreviewCallback = PreviewCallback(mConfigManager)
        mAutoFocusCallback = AutoFocusCallback()
    }
}