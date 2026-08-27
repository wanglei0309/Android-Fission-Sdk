// SPDX-FileCopyrightText: 2025 Infineon Technologies AG
//
// SPDX-License-Identifier: MIT

/**
 * Description: PinEntryEditText.kt is a custom AppCompatEditText that provides a secure and visually appealing PIN input field.
 * It supports character masking dots or asterisks per-digit boxes or underlines,
 * animations for typed characters, and validation through a listener when the full PIN is entered.
 **/
package com.infineon.secora.wallet.ui.widget

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.text.InputType
import android.text.TextUtils
import android.util.AttributeSet
import android.util.TypedValue
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.animation.OvershootInterpolator
import androidx.appcompat.widget.AppCompatEditText
import androidx.core.content.ContextCompat
import androidx.core.text.layoutDirection
import com.infineon.secora.wallet.R
import java.util.Locale

/**
 * A custom EditText view designed specifically for PIN entry.
 * Displays input as discrete character boxes with configurable styling, spacing,
 * masking, animation, and error indication support.
 *
 * Useful for secure input like 4- or 6-digit PINs or OTP fields.
 *
 * @constructor Creates a new instance of [PinEntryEditText].
 * Supports programmatic and XML-based creation.
 */
open class PinEntryEditText : AppCompatEditText {
    private var mMask: String? = null
    private var mMaskChars: StringBuilder? = null
    private var mSingleCharHint: String? = null
    private var mAnimatedType: Int = 0
    private var mSpace: Float = 24f //24 dp by default, space between the lines
    private var mCharSize: Float = 0f
    private var mNumChars: Float = 4f
    private var mTextBottomPadding: Float = 8f //8dp by default, the height of the text from our lines
    private var mMaxLength: Int = 4
    private var mLineCoords: Array<RectF?>? = null
    private lateinit var mCharBottom: FloatArray
    private var mCharPaint: Paint? = null
    private var mLastCharPaint: Paint? = null
    private var mSingleCharPaint: Paint? = null
    private var mPinBackground: Drawable? = null
    private var mTextHeight: Rect = Rect()
    private var mIsDigitSquare: Boolean = false

    private var mClickListener: OnClickListener? = null
    private var mOnPinEnteredListener: OnPinEnteredListener? = null

    private var mLineStroke: Float = 1f //1dp by default
    private var mLineStrokeSelected: Float = 2f //2dp by default
    private var mLinesPaint: Paint? = null
    private var mAnimate: Boolean = false
    var isError: Boolean = false
    private var mOriginalTextColors: ColorStateList? = null
    private var textWidths: FloatArray = FloatArray(0)
    private var mStates: Array<IntArray> = arrayOf(
        intArrayOf(android.R.attr.state_selected),  // selected
        intArrayOf(android.R.attr.state_active),  // error
        intArrayOf(android.R.attr.state_focused),  // focused
        intArrayOf(-android.R.attr.state_focused),  // unfocused
    )

    private var mColors: IntArray = intArrayOf(
        Color.GREEN,
        Color.RED,
        Color.BLACK,
        Color.GRAY
    )

    private var mColorStates: ColorStateList = ColorStateList(mStates, mColors)

    /**
     * Primary constructor used when instantiating programmatically.
     */
    constructor(context: Context?) : super(context!!)

    /**
     * XML constructor.
     *
     * @param context Context of the view.
     * @param attrs AttributeSet containing custom XML attributes.
     */
    constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
        init(context, attrs)
    }


    /**
     * XML constructor with style.
     *
     * @param context Context of the view.
     * @param attrs AttributeSet containing custom XML attributes.
     * @param defStyleAttr Default style resource.
     */
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        init(context, attrs)
    }

    /**
     * Sets a character mask used to obscure the entered PIN digits.
     * If set, all visible characters in the input field will be replaced by the mask character (e.g., '*').
     *
     * @param mask The mask character to use. If `null`, masking is disabled.
     */
    private fun setMask(mask: String?) {
        mMask = mask
        mMaskChars = null
        invalidate()
    }

    /**
     * Initializes the custom PIN input view with attributes defined in XML.
     * Sets up spacing, stroke widths, mask characters, background, and color states.
     *
     * @param context The context associated with the view.
     * @param attrs The attribute set containing custom XML attributes for configuration.
     */
    private fun init(context: Context, attrs: AttributeSet) {
        val multi = context.resources.displayMetrics.density
        mLineStroke *= multi
        mLineStrokeSelected *= multi
        mSpace *= multi //convert to pixels for our density
        mTextBottomPadding *= multi //convert to pixels for our density

        val ta = context.obtainStyledAttributes(attrs, R.styleable.PinEntryEditText, 0, 0)
        try {
            val outValue = TypedValue()
            ta.getValue(R.styleable.PinEntryEditText_pinAnimationType, outValue)
            mAnimatedType = outValue.data
            mMask = ta.getString(R.styleable.PinEntryEditText_pinCharacterMask)
            mSingleCharHint = ta.getString(R.styleable.PinEntryEditText_pinRepeatedHint)
            mLineStroke = ta.getDimension(R.styleable.PinEntryEditText_pinLineStroke, mLineStroke)
            mLineStrokeSelected = ta.getDimension(
                R.styleable.PinEntryEditText_pinLineStrokeSelected,
                mLineStrokeSelected
            )
            mSpace = ta.getDimension(R.styleable.PinEntryEditText_pinCharacterSpacing, mSpace)
            mTextBottomPadding = ta.getDimension(
                R.styleable.PinEntryEditText_pinTextBottomPadding,
                mTextBottomPadding
            )
            mIsDigitSquare =
                ta.getBoolean(R.styleable.PinEntryEditText_pinBackgroundIsSquare, mIsDigitSquare)
            mPinBackground = ta.getDrawable(R.styleable.PinEntryEditText_pinBackgroundDrawable)
            val colors = ta.getColorStateList(R.styleable.PinEntryEditText_pinLineColors)
            if (colors != null) {
                mColorStates = colors
            }
        } finally {
            ta.recycle()
        }

        mCharPaint = Paint(paint)
        mLastCharPaint = Paint(paint)
        mSingleCharPaint = Paint(paint)
        mLinesPaint = Paint(paint)
        mLinesPaint!!.strokeWidth = mLineStroke

        val outValue = TypedValue()
        context.theme.resolveAttribute(
            android.R.attr.colorAccent,
            outValue, true
        )
        val colorSelected = outValue.data
        mColors[0] = colorSelected

        val colorFocused =
            if (isInEditMode) Color.GRAY else ContextCompat.getColor(context, R.color.pin_normal)
        mColors[1] = colorFocused

        val colorUnfocused =
            if (isInEditMode) Color.GRAY else ContextCompat.getColor(context, R.color.pin_normal)
        mColors[2] = colorUnfocused

        setBackgroundResource(0)

        mMaxLength = attrs.getAttributeIntValue(XML_NAMESPACE_ANDROID, "maxLength", 4)
        mNumChars = mMaxLength.toFloat()

        //Disable copy paste
        super.setCustomSelectionActionModeCallback(object : ActionMode.Callback {
            override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onDestroyActionMode(mode: ActionMode) = Unit

            override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
                return false
            }

            override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
                return false
            }
        })
        // When tapped, move cursor to end of text.
        super.setOnClickListener { v ->
            setSelection(text!!.length)
            if (mClickListener != null) {
                mClickListener!!.onClick(v)
            }
        }

        super.setOnLongClickListener {
            setSelection(text!!.length)
            true
        }

        //If the input type is password and no mask is set, use a default mask
        if ((inputType and InputType.TYPE_TEXT_VARIATION_PASSWORD) == InputType.TYPE_TEXT_VARIATION_PASSWORD && TextUtils.isEmpty(
                mMask
            )
        ) {
            mMask = DEFAULT_MASK
        } else if ((inputType and InputType.TYPE_NUMBER_VARIATION_PASSWORD) == InputType.TYPE_NUMBER_VARIATION_PASSWORD && TextUtils.isEmpty(
                mMask
            )
        ) {
            mMask = DEFAULT_MASK
        }

        if (!TextUtils.isEmpty(mMask)) {
            mMaskChars = maskChars
        }

        //Height of the characters, used if there is a background drawable
        paint.getTextBounds("|", 0, 1, mTextHeight)

        mAnimate = mAnimatedType > -1
    }

    /**
     * Overrides the default input type behavior to automatically apply or remove a mask.
     * If the input type is a password variant and no mask has been set, a default mask is applied.
     *
     * @param type The new input type to be set.
     */
    override fun setInputType(type: Int) {
        super.setInputType(type)

        if ((type and InputType.TYPE_TEXT_VARIATION_PASSWORD) == InputType.TYPE_TEXT_VARIATION_PASSWORD
            || (type and InputType.TYPE_NUMBER_VARIATION_PASSWORD) == InputType.TYPE_NUMBER_VARIATION_PASSWORD
        ) {
            // If the input type is password and no mask is set, use a default mask
            if (TextUtils.isEmpty(mMask)) {
                setMask(DEFAULT_MASK)
            }
        } else {
            // If input type is not password, remove mask
            setMask(null)
        }
    }

    /**
     * Called when the view's size changes. Calculates dimensions and coordinates for each PIN character slot,
     * including drawing bounds and padding.
     *
     * @param w Current width of the view.
     * @param h Current height of the view.
     * @param oldw Previous width of the view.
     * @param oldh Previous height of the view.
     */
    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        mOriginalTextColors = textColors
        if (mOriginalTextColors != null) {
            mLastCharPaint!!.color = mOriginalTextColors!!.defaultColor
            mCharPaint!!.color = mOriginalTextColors!!.defaultColor
            mSingleCharPaint!!.color = currentHintTextColor
        }
        val availableWidth = width - paddingStart - paddingEnd
        mCharSize = if (mSpace < 0) {
            availableWidth / (mNumChars * 2 - 1)
        } else {
            (availableWidth - (mSpace * (mNumChars - 1))) / mNumChars
        }
        mLineCoords = arrayOfNulls(mNumChars.toInt())
        mCharBottom = FloatArray(mNumChars.toInt())
        var startX: Int
        val bottom = height - paddingBottom
        val rtlFlag: Int
        val isLayoutRtl =
            Locale.getDefault().layoutDirection == LAYOUT_DIRECTION_RTL
        if (isLayoutRtl) {
            rtlFlag = -1
            startX = (width - paddingStart - mCharSize).toInt()
        } else {
            rtlFlag = 1
            startX = paddingStart
        }
        var i = 0
        while (i < mNumChars) {
            mLineCoords!![i] =
                RectF(startX.toFloat(), bottom.toFloat(), startX + mCharSize, bottom.toFloat())
            if (mPinBackground != null) {
                if (mIsDigitSquare) {
                    mLineCoords!![i]!!.top = paddingTop.toFloat()
                    mLineCoords!![i]!!.right = startX + mLineCoords!![i]!!.height()
                } else {
                    mLineCoords!![i]!!.top -= mTextHeight.height() + mTextBottomPadding * 2
                }
            }

            startX = if (mSpace < 0) {
                (startX + rtlFlag * mCharSize * 2).toInt()
            } else {
                (startX + rtlFlag * (mCharSize + mSpace)).toInt()
            }
            mCharBottom[i] = mLineCoords!![i]!!.bottom - mTextBottomPadding
            i++
        }
    }

    /**
     * Sets a custom click listener for the PIN input view.
     * Overrides the default `EditText` behavior to store the listener for internal use.
     *
     * @param l The click listener to be set.
     */
    override fun setOnClickListener(l: OnClickListener?) {
        mClickListener = l
    }

    /**
     * Prevents selection action mode (copy/paste). Always throws an exception if called.
     *
     * @throws RuntimeException Always thrown to prevent text selection features.
     */
    override fun setCustomSelectionActionModeCallback(actionModeCallback: ActionMode.Callback?) {
        throw RuntimeException("setCustomSelectionActionModeCallback() not supported.")
    }

    /**
     * Ensures that the textWidths array has sufficient capacity for the given text length.
     *
     * This method avoids allocating a new FloatArray during draw/layout operations
     * (such as onDraw) by reusing the existing array whenever possible.
     * A new array is created only when the required size exceeds the current capacity,
     * helping to reduce unnecessary object allocations and GC overhead.
     *
     * @param requiredSize Number of characters whose widths need to be measured
     */
    private fun ensureTextWidths(requiredSize: Int) {
        if (textWidths.size < requiredSize) {
            textWidths = FloatArray(requiredSize)
        }
    }

    /**
     * Draws the PIN view with characters, background, hints, and lines.
     */
    override fun onDraw(canvas: Canvas) {
        val text = fullText ?: return
        val textLength = text.length
        ensureTextWidths(textLength)
        paint.getTextWidths(text, 0, textLength, textWidths)

        val hintWidth = calculateHintWidth()

        for (i in 0 until mNumChars.toInt()) {
            drawBackground(canvas, i, textLength)
            drawCharacterOrHint(canvas, i, text, textLength, textWidths, hintWidth)
            drawLine(canvas, i, textLength)
        }
    }

    /**
     * Calculates the total width of the hint character (if set).
     */
    private fun calculateHintWidth(): Float {
        if (mSingleCharHint == null) return 0f
        val hintWidths = FloatArray(mSingleCharHint!!.length)
        paint.getTextWidths(mSingleCharHint, hintWidths)
        return hintWidths.sum()
    }

    /**
     * Draws the background behind each character if a background drawable is set.
     */
    private fun drawBackground(canvas: Canvas, index: Int, textLength: Int) {
        mPinBackground?.let {
            updateDrawableState(index < textLength, index == textLength)
            it.setBounds(
                mLineCoords!![index]!!.left.toInt(),
                mLineCoords!![index]!!.top.toInt(),
                mLineCoords!![index]!!.right.toInt(),
                mLineCoords!![index]!!.bottom.toInt()
            )
            it.draw(canvas)
        }
    }

    /**
     * Draws either the character from the text or a hint at the given index.
     */
    private fun drawCharacterOrHint(
        canvas: Canvas,
        index: Int,
        text: CharSequence,
        textLength: Int,
        textWidths: FloatArray,
        hintWidth: Float
    ) {
        val middle = mLineCoords!![index]!!.left + mCharSize / 2
        if (textLength > index) {
            val paintToUse =
                if (!mAnimate || index != textLength - 1) mCharPaint else mLastCharPaint
            canvas.drawText(
                text,
                index,
                index + 1,
                middle - textWidths[index] / 2,
                mCharBottom[index],
                paintToUse!!
            )
        } else if (mSingleCharHint != null) {
            canvas.drawText(
                mSingleCharHint!!,
                middle - hintWidth / 2,
                mCharBottom[index],
                mSingleCharPaint!!
            )
        }
    }

    /**
     * Draws the underline for each character if no background is used.
     */
    private fun drawLine(canvas: Canvas, index: Int, textLength: Int) {
        if (mPinBackground == null) {
            updateColorForLines(index <= textLength)
            canvas.drawLine(
                mLineCoords!![index]!!.left,
                mLineCoords!![index]!!.top,
                mLineCoords!![index]!!.right,
                mLineCoords!![index]!!.bottom,
                mLinesPaint!!
            )
        }
    }

    /**
     * Returns the full text to display in the PIN field, either the real input or the masked version.
     */
    private val fullText: CharSequence?
        get() = if (mMask == null) {
            text
        } else {
            maskChars
        }

    /**
     * Returns a [StringBuilder] filled with masked characters of the same length as the entered text.
     * This is used when masking (e.g., displaying asterisks instead of the actual input).
     */
    private val maskChars: StringBuilder
        get() {
            if (mMaskChars == null) {
                mMaskChars = StringBuilder()
            }
            val textLength = text!!.length
            while (mMaskChars!!.length != textLength) {
                if (mMaskChars!!.length < textLength) {
                    mMaskChars!!.append(mMask)
                } else {
                    mMaskChars!!.deleteCharAt(mMaskChars!!.length - 1)
                }
            }
            return mMaskChars as StringBuilder
        }

    /**
     * Retrieves a color from the [ColorStateList] based on the provided view state(s).
     *
     * @param states The view states used to retrieve a color.
     * @return A color matching the given state, or gray if none matched.
     */
    private fun getColorForState(vararg states: Int): Int {
        return mColorStates.getColorForState(states, Color.GRAY)
    }

    /**
     * @param hasTextOrIsNext Is the color for a character that has been typed or is
     * the next character to be typed?
     */
    protected fun updateColorForLines(hasTextOrIsNext: Boolean) {
        if (isError) {
            mLinesPaint!!.color = getColorForState(android.R.attr.state_active)
        } else if (isFocused) {
            mLinesPaint!!.strokeWidth = mLineStrokeSelected
            mLinesPaint!!.color = getColorForState(android.R.attr.state_focused)
            if (hasTextOrIsNext) {
                mLinesPaint!!.color = getColorForState(android.R.attr.state_selected)
            }
        } else {
            mLinesPaint!!.strokeWidth = mLineStroke
            mLinesPaint!!.color = getColorForState(-android.R.attr.state_focused)
        }
    }

    /**
     * Updates the background drawable state for each character slot based on its status:
     * - Focused
     * - Selected (current input position)
     * - Error
     *
     * @param hasText `true` if the character slot already has input.
     * @param isNext `true` if the character slot is the current position for input.
     */
    protected fun updateDrawableState(hasText: Boolean, isNext: Boolean) {
        if (isError) {
            mPinBackground!!.setState(intArrayOf(android.R.attr.state_active))
        } else if (isFocused) {
            mPinBackground!!.setState(intArrayOf(android.R.attr.state_focused))
            if (isNext) {
                mPinBackground!!.setState(
                    intArrayOf(
                        android.R.attr.state_focused,
                        android.R.attr.state_selected
                    )
                )
            } else if (hasText) {
                mPinBackground!!.setState(
                    intArrayOf(
                        android.R.attr.state_focused,
                        android.R.attr.state_checked
                    )
                )
            }
        } else {
            mPinBackground!!.setState(intArrayOf(-android.R.attr.state_focused))
        }
    }

    /**
     * Called when the text is changed in the input field. Triggers animations if enabled,
     * and notifies the listener when the PIN entry is complete.
     *
     * @param text The new text content.
     * @param start The start index of the change.
     * @param lengthBefore The length of the text before the change.
     * @param lengthAfter The length of the text after the change.
     */
    override fun onTextChanged(
        text: CharSequence,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        isError = false
        if (mLineCoords == null || !mAnimate) {
            if (mOnPinEnteredListener != null && text.length == mMaxLength) {
                mOnPinEnteredListener!!.onPinEntered(text)
            }
            return
        }

        if (mAnimatedType == -1) {
            invalidate()
            return
        }

        if (lengthAfter > lengthBefore) {
            if (mAnimatedType == 0) {
                animatePopIn()
            } else {
                animateBottomUp(text, start)
            }
        }
    }

    /**
     * Runs a pop-in scale animation on the last entered character.
     * Triggers the `OnPinEnteredListener` callback if the PIN is complete.
     */
    private fun animatePopIn() {
        val va = ValueAnimator.ofFloat(1f, paint.textSize)
        va.setDuration(200)
        va.interpolator = OvershootInterpolator()
        va.addUpdateListener { animation ->
            mLastCharPaint!!.textSize = (animation.animatedValue as Float)
            this@PinEntryEditText.invalidate()
        }
        if (text!!.length == mMaxLength && mOnPinEnteredListener != null) {
            va.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) = Unit

                override fun onAnimationEnd(animation: Animator) {
                    mOnPinEnteredListener!!.onPinEntered(text)
                }

                override fun onAnimationCancel(animation: Animator) = Unit

                override fun onAnimationRepeat(animation: Animator) = Unit
            })
        }
        va.start()
    }

    /**
     * Runs a bottom-up animation combined with a fade-in effect on the newly entered character.
     * Triggers the `OnPinEnteredListener` callback if the PIN is complete.
     *
     * @param text The current text in the PIN field.
     * @param start The index of the character that was just added.
     */
    private fun animateBottomUp(text: CharSequence, start: Int) {
        mCharBottom[start] = mLineCoords!![start]!!.bottom - mTextBottomPadding
        val animUp = ValueAnimator.ofFloat(mCharBottom[start] + paint.textSize, mCharBottom[start])
        animUp.setDuration(300)
        animUp.interpolator = OvershootInterpolator()
        animUp.addUpdateListener { animation ->
            val value = animation.animatedValue as Float
            mCharBottom[start] = value
            this@PinEntryEditText.invalidate()
        }

        mLastCharPaint!!.alpha = 255
        val animAlpha = ValueAnimator.ofInt(0, 255)
        animAlpha.setDuration(300)
        animAlpha.addUpdateListener { animation ->
            val value = animation.animatedValue as Int
            mLastCharPaint!!.alpha = value
        }

        val set = AnimatorSet()
        if (text.length == mMaxLength && mOnPinEnteredListener != null) {
            set.addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) = Unit

                override fun onAnimationEnd(animation: Animator) {
                    mOnPinEnteredListener!!.onPinEntered(getText())
                }

                override fun onAnimationCancel(animation: Animator) = Unit

                override fun onAnimationRepeat(animation: Animator) = Unit
            })
        }
        set.playTogether(animUp, animAlpha)
        set.start()
    }

    /**
     * Enables or disables character animation when typing in the PIN field.
     *
     * @param animate `true` to enable animations; `false` to disable.
     */
    fun setAnimateText(animate: Boolean) {
        mAnimate = animate
    }

    /**
     * Sets a listener that will be triggered when the user finishes entering the PIN.
     *
     * @param l The [OnPinEnteredListener] to notify when PIN input is complete.
     */
    fun setOnPinEnteredListener(l: OnPinEnteredListener) {
        mOnPinEnteredListener = l
    }

    /**
     * Interface definition for a callback to be invoked when the PIN has been fully entered.
     */
    fun interface OnPinEnteredListener {
        fun onPinEntered(str: CharSequence?)
    }

    companion object {
        /**
         * Android XML namespace used in layout attributes.
         */
        private const val XML_NAMESPACE_ANDROID = "http://schemas.android.com/apk/res/android"

        /**
         * Default mask character (●) used for hiding sensitive information, e.g., passwords.
         */
        const val DEFAULT_MASK: String = "\u25CF"
    }
}