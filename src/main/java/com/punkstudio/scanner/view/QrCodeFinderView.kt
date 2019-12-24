package com.punkstudio.scanner.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.RelativeLayout
import com.punkstudio.scanner.R
import com.punkstudio.scanner.utils.ScreenUtils.Companion.getScreenWidth

/**
 * @author Mason
 *
 * @date 2019-03-05
 */
class QrCodeFinderView @JvmOverloads constructor(
    private val mContext: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RelativeLayout(mContext, attrs, defStyleAttr) {
    private val mPaint: Paint = Paint()
    private var mScannerAlpha: Int
    private val mMaskColor: Int
    private val mFrameColor: Int
    private val mLaserColor: Int
    private val mTextColor: Int
    private var mFrameRect: Rect? = null
    private val mFocusThick: Int
    private val mAngleThick: Int
    private val mAngleLength: Int
    private fun init(context: Context) {
        if (isInEditMode) {
            return
        }
        setWillNotDraw(false)
        val inflater = LayoutInflater.from(context)
        val relativeLayout = inflater.inflate(
            R.layout.layout_qr_code_scanner,
            this
        ) as RelativeLayout
        val frameLayout =
            relativeLayout.findViewById<View>(R.id.qrCodeFl) as FrameLayout
        mFrameRect = Rect()
        val layoutParams =
            frameLayout.layoutParams as LayoutParams
        mFrameRect!!.left =
            (getScreenWidth(context) - layoutParams.width) / 2
        mFrameRect!!.top = layoutParams.topMargin
        mFrameRect!!.right = mFrameRect!!.left + layoutParams.width
        mFrameRect!!.bottom = mFrameRect!!.top + layoutParams.height
    }

    public override fun onDraw(canvas: Canvas) {
        if (isInEditMode) {
            return
        }
        val frame = mFrameRect ?: return
        val width = width
        val height = height
        mPaint.color = mMaskColor
        canvas.drawRect(0f, 0f, width.toFloat(), frame.top.toFloat(), mPaint)
        canvas.drawRect(
            0f,
            frame.top.toFloat(),
            frame.left.toFloat(),
            frame.bottom + 1.toFloat(),
            mPaint
        )
        canvas.drawRect(
            frame.right + 1.toFloat(),
            frame.top.toFloat(),
            width.toFloat(),
            frame.bottom + 1.toFloat(),
            mPaint
        )
        canvas.drawRect(0f, frame.bottom + 1.toFloat(), width.toFloat(), height.toFloat(), mPaint)
        drawFocusRect(canvas, frame)
        drawAngle(canvas, frame)
        drawText(canvas, frame)
        drawLaser(canvas, frame)
        postInvalidateDelayed(
            ANIMATION_DELAY,
            frame.left,
            frame.top,
            frame.right,
            frame.bottom
        )
    }

    private fun drawFocusRect(
        canvas: Canvas,
        rect: Rect
    ) {
        mPaint.color = mFrameColor
        //Up
        canvas.drawRect(
            rect.left + mAngleLength.toFloat(),
            rect.top.toFloat(),
            rect.right - mAngleLength.toFloat(),
            rect.top + mFocusThick.toFloat(),
            mPaint
        )
        //Left
        canvas.drawRect(
            rect.left.toFloat(),
            rect.top + mAngleLength.toFloat(),
            rect.left + mFocusThick.toFloat(),
            rect.bottom - mAngleLength.toFloat(),
            mPaint
        )
        //Right
        canvas.drawRect(
            rect.right - mFocusThick.toFloat(),
            rect.top + mAngleLength.toFloat(),
            rect.right.toFloat(),
            rect.bottom - mAngleLength.toFloat(),
            mPaint
        )
        //Down
        canvas.drawRect(
            rect.left + mAngleLength.toFloat(),
            rect.bottom - mFocusThick.toFloat(),
            rect.right - mAngleLength.toFloat(),
            rect.bottom.toFloat(),
            mPaint
        )
    }

    /**
     * Draw four purple angles
     *
     * @param canvas
     * @param rect
     */
    private fun drawAngle(
        canvas: Canvas,
        rect: Rect
    ) {
        mPaint.color = mLaserColor
        mPaint.alpha = OPAQUE
        mPaint.style = Paint.Style.FILL
        mPaint.strokeWidth = mAngleThick.toFloat()
        val left = rect.left
        val top = rect.top
        val right = rect.right
        val bottom = rect.bottom
        // Top left angle
        canvas.drawRect(
            left.toFloat(),
            top.toFloat(),
            left + mAngleLength.toFloat(),
            top + mAngleThick.toFloat(),
            mPaint
        )
        canvas.drawRect(
            left.toFloat(),
            top.toFloat(),
            left + mAngleThick.toFloat(),
            top + mAngleLength.toFloat(),
            mPaint
        )
        // Top right angle
        canvas.drawRect(
            right - mAngleLength.toFloat(),
            top.toFloat(),
            right.toFloat(),
            top + mAngleThick.toFloat(),
            mPaint
        )
        canvas.drawRect(
            right - mAngleThick.toFloat(),
            top.toFloat(),
            right.toFloat(),
            top + mAngleLength.toFloat(),
            mPaint
        )
        // bottom left angle
        canvas.drawRect(
            left.toFloat(),
            bottom - mAngleLength.toFloat(),
            left + mAngleThick.toFloat(),
            bottom.toFloat(),
            mPaint
        )
        canvas.drawRect(
            left.toFloat(),
            bottom - mAngleThick.toFloat(),
            left + mAngleLength.toFloat(),
            bottom.toFloat(),
            mPaint
        )
        // bottom right angle
        canvas.drawRect(
            right - mAngleLength.toFloat(),
            bottom - mAngleThick.toFloat(),
            right.toFloat(),
            bottom.toFloat(),
            mPaint
        )
        canvas.drawRect(
            right - mAngleThick.toFloat(),
            bottom - mAngleLength.toFloat(),
            right.toFloat(),
            bottom.toFloat(),
            mPaint
        )
    }

    private fun drawText(
        canvas: Canvas,
        rect: Rect
    ) {
        val margin = 40
        mPaint.color = mTextColor
        mPaint.textSize = resources.getDimension(R.dimen.text_size_13sp)
        val text =
            resources.getString(R.string.qr_code_auto_scan_notification)
        val fontMetrics = mPaint.fontMetrics
        val fontTotalHeight = fontMetrics.bottom - fontMetrics.top
        val offY = fontTotalHeight / 2 - fontMetrics.bottom
        val newY = rect.bottom + margin + offY
        val screenScale = mContext.resources.displayMetrics.density
        val left =
            (getScreenWidth(mContext) - mPaint.textSize * text.length) / 2
        val correctedLeft = left + 55 * screenScale
        canvas.drawText(text, correctedLeft, newY, mPaint)
    }

    private fun drawLaser(
        canvas: Canvas,
        rect: Rect
    ) {
        mPaint.color = mLaserColor
        mPaint.alpha = SCANNER_ALPHA[mScannerAlpha]
        mScannerAlpha = (mScannerAlpha + 1) % SCANNER_ALPHA.size
        val middle = rect.height() / 2 + rect.top
        canvas.drawRect(
            rect.left + 2.toFloat(),
            middle - 1.toFloat(),
            rect.right - 1.toFloat(),
            middle + 2.toFloat(),
            mPaint
        )
    }

    companion object {
        private val SCANNER_ALPHA = intArrayOf(0, 64, 128, 192, 255, 192, 128, 64)
        private const val ANIMATION_DELAY = 100L
        private const val OPAQUE = 0xFF
    }

    init {
        val resources = resources
        mMaskColor = resources.getColor(R.color.qr_code_finder_mask)
        mFrameColor = resources.getColor(R.color.qr_code_finder_frame)
        mLaserColor = resources.getColor(R.color.qr_code_finder_laser)
        mTextColor = resources.getColor(R.color.qr_code_white)
        mFocusThick = 1
        mAngleThick = 8
        mAngleLength = 40
        mScannerAlpha = 0
        init(mContext)
    }
}