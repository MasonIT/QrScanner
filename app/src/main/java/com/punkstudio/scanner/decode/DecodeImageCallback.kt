package com.punkstudio.scanner.decode

import com.google.zxing.Result

interface DecodeImageCallback {
    fun decodeSucceed(result: Result?)
    fun decodeFail(type: Int, reason: String?)
}