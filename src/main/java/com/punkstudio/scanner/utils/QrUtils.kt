package com.punkstudio.scanner.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import com.google.zxing.*
import com.google.zxing.common.GlobalHistogramBinarizer
import com.google.zxing.qrcode.QRCodeReader
import java.util.*

/**
 * @author Mason
 *
 * @date 2019-03-05
 */
object QrUtils {
    private var yuvs: ByteArray? = null
    /**
     * YUV420sp
     *
     * @param inputWidth
     * @param inputHeight
     * @param scaled
     * @return
     */
    fun getYUV420sp(
        inputWidth: Int,
        inputHeight: Int,
        scaled: Bitmap
    ): ByteArray? {
        val argb = IntArray(inputWidth * inputHeight)
        scaled.getPixels(argb, 0, inputWidth, 0, 0, inputWidth, inputHeight)
        val requiredWidth = if (inputWidth % 2 == 0) inputWidth else inputWidth + 1
        val requiredHeight = if (inputHeight % 2 == 0) inputHeight else inputHeight + 1
        val byteLength = requiredWidth * requiredHeight * 3 / 2
        if (yuvs == null || yuvs!!.size < byteLength) {
            yuvs = ByteArray(byteLength)
        } else {
            Arrays.fill(yuvs, 0.toByte())
        }
        encodeYUV420SP(yuvs, argb, inputWidth, inputHeight)
        scaled.recycle()
        return yuvs
    }

    /**
     * RGB TO YUV420sp
     *
     * @param yuv420sp inputWidth * inputHeight * 3 / 2
     * @param argb inputWidth * inputHeight
     * @param width
     * @param height
     */
    private fun encodeYUV420SP(
        yuv420sp: ByteArray?,
        argb: IntArray,
        width: Int,
        height: Int
    ) {
        val frameSize = width * height
        var Y: Int
        var U: Int
        var V: Int
        var yIndex = 0
        var uvIndex = frameSize
        // int a, R, G, B;
        var R: Int
        var G: Int
        var B: Int
        //
        var argbIndex = 0
        //
        for (j in 0 until height) {
            for (i in 0 until width) { // a is not used obviously
// a = (argb[argbIndex] & 0xff000000) >> 24;
                R = argb[argbIndex] and 0xff0000 shr 16
                G = argb[argbIndex] and 0xff00 shr 8
                B = argb[argbIndex] and 0xff
                //
                argbIndex++
                // well known RGB to YUV algorithm
                Y = (66 * R + 129 * G + 25 * B + 128 shr 8) + 16
                U = (-38 * R - 74 * G + 112 * B + 128 shr 8) + 128
                V = (112 * R - 94 * G - 18 * B + 128 shr 8) + 128
                //
                Y = Math.max(0, Math.min(Y, 255))
                U = Math.max(0, Math.min(U, 255))
                V = Math.max(0, Math.min(V, 255))
                // NV21 has a plane of Y and interleaved planes of VU each sampled by a factor of 2
// meaning for every 4 Y pixels there are 1 V and 1 U. Note the sampling is every other
// pixel AND every other scanline.
// ---Y---
                yuv420sp!![yIndex++] = Y.toByte()
                // ---UV---
                if (j % 2 == 0 && i % 2 == 0) { //
                    yuv420sp[uvIndex++] = V.toByte()
                    //
                    yuv420sp[uvIndex++] = U.toByte()
                }
            }
        }
    }

    fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int { // Raw height and width of image
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2
            // Calculate the largest inSampleSize value that is a power of 2 and keeps both
// height and width larger than the requested height and width.
            while (halfHeight / inSampleSize > reqHeight && halfWidth / inSampleSize > reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    fun decodeSampledBitmapFromFile(
        imgPath: String?,
        reqWidth: Int,
        reqHeight: Int
    ): Bitmap { // First decode with inJustDecodeBounds=true to check dimensions
        val options =
            BitmapFactory.Options()
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(imgPath, options)
        // Calculate inSampleSize
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight)
        // Decode bitmap with inSampleSize set
        options.inJustDecodeBounds = false
        return BitmapFactory.decodeFile(imgPath, options)
    }

    /**
     * Decode the data within the viewfinder rectangle, and time how long it took. For efficiency, reuse the same reader
     * objects from one decode to the next.
     */
    fun decodeImage(data: ByteArray?, width: Int, height: Int): Result? {
        var result: Result? = null
        try {
            val hints =
                Hashtable<DecodeHintType, Any?>()
            hints[DecodeHintType.CHARACTER_SET] = "utf-8"
            hints[DecodeHintType.TRY_HARDER] = true
            hints[DecodeHintType.POSSIBLE_FORMATS] = BarcodeFormat.QR_CODE
            val source =
                PlanarYUVLuminanceSource(data, width, height, 0, 0, width, height, false)
            val bitmap1 = BinaryBitmap(GlobalHistogramBinarizer(source))
            // BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
            val reader2 = QRCodeReader()
            result = reader2.decode(bitmap1, hints)
        } catch (e: ReaderException) {
        }
        return result
    }
}