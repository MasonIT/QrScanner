package com.punkstudio.scanner

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.Vibrator
import android.provider.MediaStore
import android.text.TextUtils
import android.util.Log
import android.view.SurfaceHolder
import android.view.View
import android.view.Window
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.zxing.Result
import com.punkstudio.scanner.camera.CameraManager.Companion.get
import com.punkstudio.scanner.camera.CameraManager.Companion.init
import com.punkstudio.scanner.decode.*
import com.punkstudio.scanner.decode.DecodeManager.OnRefreshCameraListener
import kotlinx.android.synthetic.main.activity_qr_code.*
import java.io.IOException
import java.lang.ref.WeakReference
import java.util.concurrent.Executor
import java.util.concurrent.Executors

/**
 * @author Mason
 *
 * @date 2019-03-05
 */
class ScannerActivity : AppCompatActivity(), SurfaceHolder.Callback,
    View.OnClickListener {
    private var mCaptureActivityHandler: CaptureActivityHandler? = null
    private var mHasSurface = false
    private var mPermissionOk = false
    private var mInactivityTimer: InactivityTimer? = null
    private val mDecodeManager = DecodeManager()
    private var mMediaPlayer: MediaPlayer? = null
    private var mPlayBeep = false
    private var mVibrate = false
    private var mNeedFlashLightOpen = true
    private var mQrCodeExecutor: Executor? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_qr_code)
        initView()
        initData()
    }

    private fun checkPermission() {
        val hasHardware = checkCameraHardWare(this)
        if (hasHardware) {
            if (!hasCameraPermission()) {
                qrCodeBg.visibility = View.VISIBLE
                qrFinder.visibility = View.GONE
                mPermissionOk = false
            } else {
                mPermissionOk = true
            }
        } else {
            mPermissionOk = false
            finish()
        }
    }

    private fun initView() {
        mHasSurface = false
        qrLight.setOnClickListener(this)
        qrPicture.setOnClickListener(this)
    }

    private fun initData() {
        init(this)
        mInactivityTimer = InactivityTimer(this@ScannerActivity)
        mQrCodeExecutor = Executors.newSingleThreadExecutor()
    }

    private fun hasCameraPermission(): Boolean {
        val pm = packageManager
        return PackageManager.PERMISSION_GRANTED == pm.checkPermission(
            "android.permission.CAMERA",
            packageName
        )
    }

    override fun onResume() {
        super.onResume()
        checkPermission()
        if (!mPermissionOk) {
            mDecodeManager.showPermissionDeniedDialog(this)
            return
        }
        val surfaceHolder = qrPreview.holder
        turnFlashLightOff()
        if (mHasSurface) {
            initCamera(surfaceHolder)
        } else {
            surfaceHolder.addCallback(this)
            surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS)
        }
        mPlayBeep = true
        val audioService =
            getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audioService.ringerMode != AudioManager.RINGER_MODE_NORMAL) {
            mPlayBeep = false
        }
        initBeepSound()
        mVibrate = true
    }

    override fun onPause() {
        super.onPause()
        if (mCaptureActivityHandler != null) {
            mCaptureActivityHandler!!.quitSynchronously()
            mCaptureActivityHandler = null
        }
        get()!!.closeDriver()
    }

    override fun onDestroy() {
        if (null != mInactivityTimer) {
            mInactivityTimer!!.shutdown()
        }
        super.onDestroy()
    }

    fun handleDecode(result: Result?) {
        mInactivityTimer!!.onActivity()
        playBeepSoundAndVibrate()
        if (null == result) {
            mDecodeManager.showCouldNotReadQrCodeFromScanner(
                this,
                object : OnRefreshCameraListener {
                    override fun refresh() {
                        restartPreview()
                    }
                })
        } else {
            val resultString = result.text
            handleResult(resultString)
        }
    }

    private fun initCamera(surfaceHolder: SurfaceHolder) {
        try {
            get()!!.openDriver(surfaceHolder)
        } catch (e: IOException) {
            Toast.makeText(
                this,
                getString(R.string.qr_code_camera_not_found),
                Toast.LENGTH_SHORT
            ).show()
            finish()
            return
        } catch (re: RuntimeException) {
            re.printStackTrace()
            mDecodeManager.showPermissionDeniedDialog(this)
            return
        }
        qrFinder.visibility = View.VISIBLE
        qrPreview.visibility = View.VISIBLE
        qrLightLl.visibility = View.VISIBLE
        qrCodeBg.visibility = View.GONE
        if (mCaptureActivityHandler == null) {
            mCaptureActivityHandler = CaptureActivityHandler(this)
        }
    }

    private fun restartPreview() {
        if (null != mCaptureActivityHandler) {
            mCaptureActivityHandler!!.restartPreviewAndDecode()
        }
    }

    override fun surfaceChanged(
        holder: SurfaceHolder,
        format: Int,
        width: Int,
        height: Int
    ) {
    }

    private fun checkCameraHardWare(context: Context): Boolean {
        val packageManager = context.packageManager
        return packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        if (!mHasSurface) {
            mHasSurface = true
            initCamera(holder)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        mHasSurface = false
    }

    val captureActivityHandler: Handler?
        get() = mCaptureActivityHandler

    private fun initBeepSound() {
        if (mPlayBeep && mMediaPlayer == null) {
            volumeControlStream = AudioManager.STREAM_MUSIC
            mMediaPlayer = MediaPlayer()
            mMediaPlayer!!.setAudioStreamType(AudioManager.STREAM_MUSIC)
            mMediaPlayer!!.setOnCompletionListener(mBeepListener)
            val file =
                resources.openRawResourceFd(R.raw.beep)
            try {
                mMediaPlayer!!.setDataSource(
                    file.fileDescriptor,
                    file.startOffset,
                    file.length
                )
                file.close()
                mMediaPlayer!!.setVolume(
                    BEEP_VOLUME,
                    BEEP_VOLUME
                )
                mMediaPlayer!!.prepare()
            } catch (e: IOException) {
                mMediaPlayer = null
            }
        }
    }

    private fun playBeepSoundAndVibrate() {
        if (mPlayBeep && mMediaPlayer != null) {
            mMediaPlayer!!.start()
        }
        if (mVibrate) {
            val vibrator =
                getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            vibrator.vibrate(VIBRATE_DURATION)
        }
    }

    /**
     * When the beep has finished playing, rewind to queue up another one.
     */
    private val mBeepListener =
        MediaPlayer.OnCompletionListener { mediaPlayer -> mediaPlayer.seekTo(0) }

    override fun onClick(v: View) {
        if (v.id == R.id.qrLight) {
            if (mNeedFlashLightOpen) {
                turnFlashlightOn()
            } else {
                turnFlashLightOff()
            }
        } else if (v.id == R.id.qrPicture) {
            if (!hasCameraPermission()) {
                mDecodeManager.showPermissionDeniedDialog(this)
            } else {
                openSystemAlbum()
            }
        }
    }

    private fun openSystemAlbum() {
        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(intent, REQUEST_SYSTEM_PICTURE)
    }

    private fun turnFlashlightOn() {
        mNeedFlashLightOpen = false
        qrLightText.text = getString(R.string.qr_code_close_flash_light)
        qrLight.setBackgroundResource(R.drawable.flashlight_turn_off)
        get()!!.setFlashLight(true)
    }

    private fun turnFlashLightOff() {
        mNeedFlashLightOpen = true
        qrLightText.text = getString(R.string.qr_code_open_flash_light)
        qrLight.setBackgroundResource(R.drawable.flashlight_turn_on)
        get()!!.setFlashLight(false)
    }

    private fun handleResult(resultString: String) {
        if (TextUtils.isEmpty(resultString)) {
            mDecodeManager.showCouldNotReadQrCodeFromScanner(
                this,
                object : OnRefreshCameraListener {
                    override fun refresh() {
                        restartPreview()
                    }
                })
        } else {
            Log.d(TAG, "Got scan result from user loaded image :$resultString")
            val data = Intent()
            data.putExtra(SCAN_RESULT, resultString)
            setResult(RESULT_OK, data)
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || data == null) {
            return
        }
        when (requestCode) {
            REQUEST_PICTURE -> finish()
            REQUEST_SYSTEM_PICTURE -> {
                val uri = data.data
                val imgPath = getPathFromUri(uri)
                if (!TextUtils.isEmpty(imgPath) && null != mQrCodeExecutor) {
                    mQrCodeExecutor!!.execute(DecodeImageThread(imgPath, mDecodeImageCallback))
                }
            }
        }
    }

    private fun getPathFromUri(uri: Uri?): String {
        var cursor =
            contentResolver.query(uri!!, null, null, null, null)
        cursor!!.moveToFirst()
        var documentId = cursor.getString(0)
        documentId = documentId.substring(documentId.lastIndexOf(":") + 1)
        cursor.close()
        cursor = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            null,
            MediaStore.Images.Media._ID + " = ? ",
            arrayOf(documentId),
            null
        )
        cursor!!.moveToFirst()
        val path =
            cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
        cursor.close()
        return path
    }

    private val mDecodeImageCallback: DecodeImageCallback = object : DecodeImageCallback {
        override fun decodeSucceed(result: Result?) {
            Log.d(TAG, "Decoded the image successfully :" + result!!.text)
            val data = Intent()
            data.putExtra(SCAN_RESULT, result.text)
            setResult(RESULT_OK, data)
            finish()
        }

        override fun decodeFail(type: Int, reason: String?) {
            Log.d(TAG, "Something went wrong decoding the image :$reason")
            val data = Intent()
            data.putExtra(SCAN_DECODING_IMAGE_ERROR, reason)
            setResult(RESULT_CANCELED, data)
            finish()
        }
    }

    private class WeakHandler(imagePickerActivity: ScannerActivity) :
        Handler() {
        private val mWeakScannerActivity: WeakReference<ScannerActivity> = WeakReference(imagePickerActivity)
        private val mDecodeManager = DecodeManager()
        override fun handleMessage(msg: Message) {
            val qrCodeActivity = mWeakScannerActivity.get()
            when (msg.what) {
                MSG_DECODE_SUCCEED -> {
                    val result = msg.obj as Result
                    if (null == result) {
                        mDecodeManager.showCouldNotReadQrCodeFromPicture(qrCodeActivity)
                    } else {
                        val resultString = result.text
                        handleResult(resultString)
                    }
                }
                MSG_DECODE_FAIL -> mDecodeManager.showCouldNotReadQrCodeFromPicture(
                    qrCodeActivity
                )
            }
            super.handleMessage(msg)
        }

        private fun handleResult(resultString: String) {
            val imagePickerActivity = mWeakScannerActivity.get()
            mDecodeManager.showResultDialog(
                imagePickerActivity,
                resultString,
                DialogInterface.OnClickListener { dialog, which -> dialog.dismiss() })
        }

    }

    companion object {
        private const val REQUEST_SYSTEM_PICTURE = 0
        private const val REQUEST_PICTURE = 1
        const val MSG_DECODE_SUCCEED = 1
        const val MSG_DECODE_FAIL = 2
        private const val BEEP_VOLUME = 0.10f
        private const val VIBRATE_DURATION = 200L
        private val TAG = ScannerActivity::class.java.simpleName
        private fun createIntent(context: Context): Intent {
            return Intent(context, ScannerActivity::class.java)
        }
    }
}