package com.punkstudio.scanner.utils

import android.content.Context

/**
 * @author Mason
 *
 * @date 2019-03-05
 */
class ScreenUtils private constructor() {
    companion object {
        @JvmStatic
        fun getScreenWidth(context: Context): Int {
            val dm = context.resources.displayMetrics
            return dm.widthPixels
        }

        fun getScreenHeight(context: Context): Int {
            val dm = context.resources.displayMetrics
            return dm.heightPixels
        }
    }

    init {
        throw AssertionError()
    }
}