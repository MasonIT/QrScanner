package com.punkstudio.scanner.decode

import android.app.Activity
import android.content.DialogInterface

/**
 * @author Mason
 *
 * @date 2019-03-05
 */
class FinishListener(private val mActivityToFinish: Activity) :
    DialogInterface.OnClickListener,
    DialogInterface.OnCancelListener, Runnable {
    override fun onCancel(dialogInterface: DialogInterface) {
        run()
    }

    override fun onClick(
        dialogInterface: DialogInterface,
        i: Int
    ) {
        run()
    }

    override fun run() {
        mActivityToFinish.finish()
    }

}