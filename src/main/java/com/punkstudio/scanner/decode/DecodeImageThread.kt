package com.punkstudio.scanner.decode

import android.text.TextUtils
import com.punkstudio.scanner.utils.QrUtils

/**
 * @author Mason
 *
 * @date 2019-03-05
 */
class DecodeImageThread(private val mImgPath: String, private val mCallback: DecodeImageCallback?) : Runnable {
    private var mData: ByteArray? = null
    private var mWidth = 0
    private var mHeight = 0
    override fun run() {
        if (null == mData) {
            if (!TextUtils.isEmpty(mImgPath)) {
                val bitmap = QrUtils.decodeSampledBitmapFromFile(
                    mImgPath,
                    MAX_PICTURE_PIXEL,
                    MAX_PICTURE_PIXEL
                )
                mData = QrUtils.getYUV420sp(bitmap.width, bitmap.height, bitmap)
                mWidth = bitmap.width
                mHeight = bitmap.height
            }
        }
        if (mData == null || mData!!.isEmpty() || mWidth == 0 || mHeight == 0) {
            mCallback?.decodeFail(0, "No image data")
            return
        }
        val result = QrUtils.decodeImage(mData, mWidth, mHeight)
        if (null != mCallback) {
            if (null != result) {
                mCallback.decodeSucceed(result)
            } else {
                mCallback.decodeFail(0, "Decode image failed.")
            }
        }
    }

    companion object {
        private const val MAX_PICTURE_PIXEL = 256
    }

}