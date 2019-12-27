package com.punkstudio.scanner.decode

import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import com.punkstudio.scanner.R

/**
 * @author Mason
 *
 * @date 2019-03-05
 */
class DecodeManager {
    fun showPermissionDeniedDialog(context: Context?) {
        Log.e("DecodeManager", "Permissions not granted")
        AlertDialog.Builder(context)
            .setTitle(R.string.qr_code_notification)
            .setMessage(R.string.qr_code_camera_not_open)
            .setPositiveButton(
                R.string.qr_code_positive_button_know
            ) { dialog, _ -> dialog.dismiss() }.show()
    }

    fun showResultDialog(
        activity: Activity?,
        resultString: String?,
        listener: DialogInterface.OnClickListener?
    ) {
        AlertDialog.Builder(activity)
            .setTitle(R.string.qr_code_notification)
            .setMessage(resultString)
            .setPositiveButton(
                R.string.qr_code_positive_button_confirm,
                listener
            ).show()
    }

    fun showCouldNotReadQrCodeFromScanner(
        context: Context?,
        listener: OnRefreshCameraListener?
    ) {
        AlertDialog.Builder(context)
            .setTitle(R.string.qr_code_notification)
            .setMessage(R.string.qr_code_could_not_read_qr_code_from_scanner)
            .setPositiveButton(
                R.string.qc_code_close
            ) { dialog, _ ->
                dialog.dismiss()
                listener?.refresh()
            }.show()
    }

    fun showCouldNotReadQrCodeFromPicture(context: Context?) {
        AlertDialog.Builder(context)
            .setTitle(R.string.qr_code_notification)
            .setMessage(R.string.qr_code_could_not_read_qr_code_from_picture)
            .setPositiveButton(
                R.string.qc_code_close
            ) { dialog, _ -> dialog.dismiss() }.show()
    }

    interface OnRefreshCameraListener {
        fun refresh()
    }
}