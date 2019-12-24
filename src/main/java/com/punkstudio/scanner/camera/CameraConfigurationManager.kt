@file:Suppress("DEPRECATION")

package com.punkstudio.scanner.camera

import android.content.Context
import android.graphics.Point
import android.hardware.Camera
import android.util.Log
import com.punkstudio.scanner.utils.ScreenUtils
import java.util.*
import java.util.regex.Pattern
import kotlin.math.abs

/**
 * @author Mason
 *
 * @date 2019-03-05
 */
@Suppress("DEPRECATION")
internal class CameraConfigurationManager(private val mContext: Context) {
    var cameraResolution: Camera.Size? = null
        private set
    private var mPictureResolution: Camera.Size? = null
    /**
     * Reads, one time, values from the camera that are needed by the app.
     */
    fun initFromCameraParameters(camera: Camera) {
        val parameters = camera.parameters
        cameraResolution = findCloselySize(
            ScreenUtils.getScreenWidth(mContext),
            ScreenUtils.getScreenHeight(mContext),
            parameters.supportedPreviewSizes
        )
        Log.e(
            TAG,
            "Setting preview size: " + cameraResolution!!.width + "-" + cameraResolution!!.height
        )
        mPictureResolution = findCloselySize(
            ScreenUtils.getScreenWidth(mContext),
            ScreenUtils.getScreenHeight(mContext),
            parameters.supportedPictureSizes
        )
        Log.e(
            TAG,
            "Setting picture size: " + mPictureResolution!!.width + "-" + mPictureResolution!!.height
        )
    }

    /**
     * Sets the camera up to take preview images which are used for both preview and decoding. We detect the preview
     * format here so that buildLuminanceSource() can build an appropriate LuminanceSource subclass. In the future we
     * may want to force YUV420SP as it's the smallest, and the planar Y can be used for barcode scanning without a copy
     * in some cases.
     */
    fun setDesiredCameraParameters(camera: Camera) {
        val parameters = camera.parameters
        parameters.setPreviewSize(cameraResolution!!.width, cameraResolution!!.height)
        parameters.setPictureSize(mPictureResolution!!.width, mPictureResolution!!.height)
        setZoom(parameters)
        camera.setDisplayOrientation(90)
        camera.parameters = parameters
    }

    private fun setZoom(parameters: Camera.Parameters) {
        val zoomSupportedString = parameters["zoom-supported"]
        if (zoomSupportedString != null && !zoomSupportedString.toBoolean()) {
            return
        }
        var tenDesiredZoom = TEN_DESIRED_ZOOM
        val maxZoomString = parameters["max-zoom"]
        if (maxZoomString != null) {
            try {
                val tenMaxZoom = (10.0 * maxZoomString.toDouble()).toInt()
                if (tenDesiredZoom > tenMaxZoom) {
                    tenDesiredZoom = tenMaxZoom
                }
            } catch (nfe: NumberFormatException) {
                Log.e(
                    TAG,
                    "Bad max-zoom: $maxZoomString"
                )
            }
        }
        val takingPictureZoomMaxString = parameters["taking-picture-zoom-max"]
        if (takingPictureZoomMaxString != null) {
            try {
                val tenMaxZoom = takingPictureZoomMaxString.toInt()
                if (tenDesiredZoom > tenMaxZoom) {
                    tenDesiredZoom = tenMaxZoom
                }
            } catch (nfe: NumberFormatException) {
                Log.e(
                    TAG,
                    "Bad taking-picture-zoom-max: $takingPictureZoomMaxString"
                )
            }
        }
        val motZoomValuesString = parameters["mot-zoom-values"]
        if (motZoomValuesString != null) {
            tenDesiredZoom = findBestMotZoomValue(
                motZoomValuesString,
                tenDesiredZoom
            )
        }
        val motZoomStepString = parameters["mot-zoom-step"]
        if (motZoomStepString != null) {
            try {
                val motZoomStep = motZoomStepString.trim { it <= ' ' }.toDouble()
                val tenZoomStep = (10.0 * motZoomStep).toInt()
                if (tenZoomStep > 1) {
                    tenDesiredZoom -= tenDesiredZoom % tenZoomStep
                }
            } catch (nfe: NumberFormatException) { // continue
            }
        }
        // Set zoom. This helps encourage the user to pull back.
// Some devices like the Behold have a zoom parameter
// if (maxZoomString != null || motZoomValuesString != null) {
// parameters.set("zoom", String.valueOf(tenDesiredZoom / 10.0));
// }
        if (parameters.isZoomSupported) {
            Log.e(
                TAG,
                "max-zoom:" + parameters.maxZoom
            )
            parameters.zoom = parameters.maxZoom / 10
        } else {
            Log.e(TAG, "Unsupported zoom.")
        }
    }

    private fun findCloselySize(
        surfaceWidth: Int,
        surfaceHeight: Int,
        preSizeList: List<Camera.Size>
    ): Camera.Size {
        Collections.sort(
            preSizeList,
            SizeComparator(surfaceWidth, surfaceHeight)
        )
        return preSizeList[0]
    }

    private class SizeComparator internal constructor(width: Int, height: Int) :
        Comparator<Camera.Size> {
        private var width = 0
        private var height = 0
        private val ratio: Float
        override fun compare(
            size1: Camera.Size,
            size2: Camera.Size
        ): Int {
            val width1 = size1.width
            val height1 = size1.height
            val width2 = size2.width
            val height2 = size2.height
            val ratio1 = abs(height1.toFloat() / width1 - ratio)
            val ratio2 = abs(height2.toFloat() / width2 - ratio)
            val result = ratio1.compareTo(ratio2)
            return if (result != 0) {
                result
            } else {
                val minGap1 = abs(width - width1) + abs(height - height1)
                val minGap2 = abs(width - width2) + abs(height - height2)
                minGap1 - minGap2
            }
        }

        init {
            if (width < height) {
                this.width = height
                this.height = width
            } else {
                this.width = width
                this.height = height
            }
            ratio = this.height.toFloat() / this.width
        }
    }

    companion object {
        private val TAG = CameraConfigurationManager::class.java.name
        private const val TEN_DESIRED_ZOOM = 10
        private val COMMA_PATTERN = Pattern.compile(",")
        private fun getCameraResolution(
            parameters: Camera.Parameters,
            screenResolution: Point
        ): Point {
            var previewSizeValueString = parameters["preview-size-values"]
            if (previewSizeValueString == null) {
                previewSizeValueString = parameters["preview-size-value"]
            }
            var cameraResolution: Point? = null
            if (previewSizeValueString != null) {
                Log.e(
                    TAG,
                    "preview-size-values parameter: $previewSizeValueString"
                )
                cameraResolution = findBestPreviewSizeValue(
                    previewSizeValueString,
                    screenResolution
                )
            }
            if (cameraResolution == null) {
                // Ensure that the camera resolution is a multiple of 8, as the screen may not be.
                cameraResolution = Point(
                    screenResolution.x shr 3 shl 3,
                    screenResolution.y shr 3 shl 3
                )
            }
            return cameraResolution
        }

        private fun findBestPreviewSizeValue(
            previewSizeValueString: CharSequence,
            screenResolution: Point
        ): Point? {
            var bestX = 0
            var bestY = 0
            var diff = Int.MAX_VALUE
            for (previewSize in COMMA_PATTERN.split(
                previewSizeValueString
            )) {
                val size = previewSize.trim { it <= ' ' }
                val dimPosition = size.indexOf('x')
                if (dimPosition < 0) {
                    Log.e(
                        TAG,
                        "Bad preview-size: $size"
                    )
                    continue
                }
                var newX: Int
                var newY: Int
                try {
                    newY = size.substring(0, dimPosition).toInt()
                    newX = size.substring(dimPosition + 1).toInt()
                } catch (nfe: NumberFormatException) {
                    Log.e(
                        TAG,
                        "Bad preview-size: $size"
                    )
                    continue
                }
                val newDiff =
                    abs(newX - screenResolution.x) + abs(newY - screenResolution.y)
                if (newDiff == 0) {
                    bestX = newX
                    bestY = newY
                    break
                } else if (newDiff < diff) {
                    bestX = newX
                    bestY = newY
                    diff = newDiff
                }
            }
            return if (bestX > 0 && bestY > 0) {
                Point(bestX, bestY)
            } else null
        }

        private fun findBestMotZoomValue(
            stringValues: CharSequence,
            tenDesiredZoom: Int
        ): Int {
            var tenBestValue = 0
            for (stringValue in COMMA_PATTERN.split(
                stringValues
            )) {
                val sValue = stringValue.trim { it <= ' ' }
                val value: Double = try {
                    sValue.toDouble()
                } catch (nfe: NumberFormatException) {
                    return tenDesiredZoom
                }
                val tenValue = (10.0 * value).toInt()
                if (abs(tenDesiredZoom - value) < abs(tenDesiredZoom - tenBestValue)) {
                    tenBestValue = tenValue
                }
            }
            return tenBestValue
        }

    }

}