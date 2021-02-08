package com.example.focusstartspeedometr

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import kotlin.math.*


class SpeedometrView @JvmOverloads constructor(
    context: Context,
    attributeSet: AttributeSet? = null,
    defStyleAttr: Int = 0,
    defStyleRes: Int = 0,
) : View(context, attributeSet, defStyleAttr, defStyleRes) {


    private companion object {
        const val SUPER_STATE = "super_state"
        const val SPEED_STATE = "speed_state"
        const val ARROW_COLOR_STATE = "arrow_color_state"
    }

    private var paint = Paint(Paint.ANTI_ALIAS_FLAG) // Сглаживание

    private var maxSpeed = 180                       // Максимум на шкале
    private var speed = 0                            // Значение скорости
    private var textStep = 20                        // Шаг нумирации

    private var fillColor = Color.DKGRAY             // Фон спидометра
    private var fillSpeedColor = Color.LTGRAY        // Фон цифрового спидометра
    private var scaleColor = Color.WHITE             // Шкала
    private var textScaleColor = Color.WHITE         // Цыфры на шкале
    private var textSpeedColor = Color.BLACK         // Скорость на шкале
    private var pointerArrowColor = Color.GREEN      // Стрелка
    private var hingeArrowColor = Color.RED          // Кружок на стрелке

    private val MAX_ANIMATION_TIME = 10_000L          // Время анимации через всю шкалу
    private val animatorSet = AnimatorSet()          // Ссылка на объект управления аниматорами


    init {
        val typedArray = context.obtainStyledAttributes(
            attributeSet,
            R.styleable.SpeedometrView,
            defStyleAttr,
            defStyleRes
        )
        try {
            maxSpeed = typedArray.getInt(R.styleable.SpeedometrView_maxSpeed, 180).coerceAtLeast(40)
            speed = typedArray.getInt(R.styleable.SpeedometrView_speed, 0).coerceAtMost(maxSpeed)
            textStep = typedArray.getInt(R.styleable.SpeedometrView_textStep, 20)
            fillColor = typedArray.getColor(R.styleable.SpeedometrView_fillColor, Color.DKGRAY)
            fillSpeedColor = typedArray.getColor(R.styleable.SpeedometrView_fillSpeedColor, Color.LTGRAY)
            scaleColor = typedArray.getColor(R.styleable.SpeedometrView_scaleColor, Color.WHITE)
            textScaleColor = typedArray.getColor(R.styleable.SpeedometrView_textScaleColor, Color.WHITE)
            textSpeedColor = typedArray.getColor(R.styleable.SpeedometrView_textSpeedColor, Color.BLACK)
            pointerArrowColor = typedArray.getColor(R.styleable.SpeedometrView_pointerArrowColor, Color.GREEN)
            hingeArrowColor = typedArray.getColor(R.styleable.SpeedometrView_scaleColor, Color.RED)
        } finally {
            typedArray.recycle()
        }
    }


    override fun onSaveInstanceState(): Parcelable? =
        Bundle().apply {
            putParcelable(SUPER_STATE, super.onSaveInstanceState())
            putInt(SPEED_STATE, speed)
            putInt(ARROW_COLOR_STATE, pointerArrowColor)
        }

    override fun onRestoreInstanceState(state: Parcelable?) {
        var superState = state

        if (state is Bundle) {
            superState = state.getParcelable(SUPER_STATE)
            speed = state.getInt(SPEED_STATE)
            pointerArrowColor = state.getInt(ARROW_COLOR_STATE)
            playAnimation(0, Color.GREEN, calculationOfAnimationTime(0))
        }

        invalidate()
        super.onRestoreInstanceState(superState)
    }


    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        var newWidth = MeasureSpec.getSize(widthMeasureSpec)

        val heigthMode = MeasureSpec.getMode(heightMeasureSpec)
        var newHeigth = MeasureSpec.getSize(heightMeasureSpec)

        if (widthMode != MeasureSpec.EXACTLY)
            newWidth = newWidth.coerceAtMost(newHeigth * 2)
        if (heigthMode != MeasureSpec.EXACTLY)
            newHeigth = newHeigth.coerceAtMost(newWidth / 2)

        setMeasuredDimension(newWidth, newHeigth)
    }

    //анимация скорости, для стрелки
    private fun speedAnimation(lastSpeed: Int, newSpeed: Int): Animator? =
        ValueAnimator.ofInt(lastSpeed, newSpeed).apply {
            addUpdateListener {
                speed = animatedValue as Int
                invalidate()
            }
            interpolator = LinearInterpolator()
        }

    // анимация цвета, для стрелки
    private fun colorAnimation(fistColor: Int, secondColor: Int): Animator? =
        ValueAnimator.ofArgb(fistColor, secondColor).apply {
            addUpdateListener {
                pointerArrowColor = animatedValue as Int
                invalidate()
            }
            setEvaluator(ArgbEvaluator())
        }

    // расчет времени анимации
    private fun calculationOfAnimationTime(finalSpeed: Int): Long {
        return ((abs(finalSpeed - speed)).toFloat() / maxSpeed.toFloat() * MAX_ANIMATION_TIME).roundToInt().toLong()
    }

    //запус анимации
    private fun playAnimation(finalSpeed: Int, finalColor: Int, time: Long) {
        if (animatorSet.isStarted) {
            animatorSet.cancel()
        }
        animatorSet.play(speedAnimation(speed, finalSpeed))
            .with(colorAnimation(pointerArrowColor, finalColor))
        animatorSet.duration = time

        animatorSet.start()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        return when (event.action) {
            MotionEvent.ACTION_DOWN -> {        // увеличение скорости
                playAnimation(maxSpeed, Color.RED, calculationOfAnimationTime(maxSpeed))
                true
            }
            MotionEvent.ACTION_UP -> {          // уменьшение скорости
                playAnimation(0, Color.GREEN, calculationOfAnimationTime(0))
                true
            }
            else -> super.onTouchEvent(event)
        }
    }

    // графическая отрисовка спидометра
    private fun drawScale(canvas: Canvas) {
        canvas.save()
        canvas.translate(width / 2f, height.toFloat())
        canvas.scale(0.5f * width, -height.toFloat())

        // фон спидометра
        paint.style = Paint.Style.FILL
        paint.color = fillColor
        canvas.drawCircle(0f, 0f, 1f, paint)

        // фон цифрового спидометра
        paint.style = Paint.Style.FILL
        paint.color = fillSpeedColor
        val halfRectWidth = (ceil(log10(abs(maxSpeed) + 0.5f)) + 2f) * 0.072f / 2
        canvas.drawRoundRect(-halfRectWidth, 0.36f, halfRectWidth, 0.14f, 0.07f, 0.08f, paint)

        // окантовка цифрового спидометра
        paint.style = Paint.Style.STROKE
        paint.color = Color.BLACK
        paint.strokeWidth = .005f
        canvas.drawRoundRect(-halfRectWidth, 0.36f, halfRectWidth, 0.14f, 0.07f, 0.08f, paint)

        // разметка шкалы
        paint.style = Paint.Style.STROKE
        paint.color = scaleColor

        val circlePadding = 0.98f
        val shortLine = 0.97f
        val mediumLine = 0.95f
        val longLine = 0.94f

        val step = (PI / maxSpeed) * (textStep / 10)
        for (i in 0..maxSpeed / (textStep / 10)) {
            val x1: Float = cos(PI - step * i).toFloat() * circlePadding
            val y1: Float = sin(PI - step * i).toFloat() * circlePadding
            var x2: Float
            var y2: Float
            when {
                i % 10 == 0 -> {
                    paint.strokeWidth = 0.02f
                    x2 = x1 * longLine
                    y2 = y1 * longLine
                }
                i % 5 == 0 -> {
                    paint.strokeWidth = 0.01f
                    x2 = x1 * mediumLine
                    y2 = y1 * mediumLine
                }
                else -> {
                    paint.strokeWidth = 0.005f
                    x2 = x1 * shortLine
                    y2 = y1 * shortLine
                }
            }
            canvas.drawLine(x1, y1, x2, y2, paint)
        }
        canvas.restore()
    }

    // Текстовая отрисовка спидометра
    private fun drawNumbersScale(canvas: Canvas) {
        canvas.save()
        canvas.translate(width / 2f, 0f)

        // шкала спидометра
        paint.style = Paint.Style.FILL
        paint.color = textScaleColor
        paint.textSize = height / 10f

        val numbersIndent = height * 0.8f
        val step = PI / maxSpeed
        var i = 0
        while (i <= maxSpeed) {
            val x = cos(PI - step * i).toFloat() * numbersIndent
            val y = sin(PI - step * i).toFloat() * numbersIndent
            val textMeasure = paint.measureText(i.toString()).toInt()
            canvas.drawText(i.toString(), x - textMeasure / 2, height - y, paint)
            i += textStep
        }

        // скорось на цифровом спидометре
        paint.color = textSpeedColor
        paint.textSize = height / 7f

        canvas.drawText(
            speed.toString(),
            -paint.measureText(speed.toString()) / 2f,
            height * 0.8f,
            paint
        )

        canvas.restore()
    }

    // отрисовка анимированного объекта - стрелки спидометра
    private fun drawArrowSpeed(canvas: Canvas) {
        canvas.save()
        canvas.translate(width / 2f, height.toFloat())
        canvas.scale(0.5f * width, -height.toFloat())
        canvas.rotate(90 - 180.toFloat() * (speed / maxSpeed.toFloat()))

        // стрелка
        paint.color = pointerArrowColor
        paint.strokeWidth = 0.02f
        canvas.drawLine(0.01f, 0f, 0f, 0.93f, paint)
        canvas.drawLine(-0.01f, 0f, 0f, 0.93f, paint)

        // основание стрелки
        paint.style = Paint.Style.FILL
        paint.color = hingeArrowColor
        canvas.drawCircle(0f, 0f, .07f, paint)

        // граница основания стрелки
        paint.style = Paint.Style.STROKE
        paint.color = fillColor
        paint.strokeWidth = .01f
        canvas.drawCircle(0f, 0f, .07f, paint)

        canvas.restore()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        drawScale(canvas)
        drawNumbersScale(canvas)
        drawArrowSpeed(canvas)
    }
}
