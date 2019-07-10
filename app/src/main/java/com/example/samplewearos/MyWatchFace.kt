package com.example.samplewearos

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*

import android.os.Bundle
import android.support.wearable.watchface.CanvasWatchFaceService
import android.support.wearable.watchface.WatchFaceService
import android.support.wearable.watchface.WatchFaceStyle
import android.view.SurfaceHolder

import java.util.Calendar
import java.util.TimeZone

private const val HOUR_STROKE_WIDTH = 7f
private const val MINUTE_STROKE_WIDTH = 6f
private const val STROKE_WIDTH = 4f

private const val CENTER_GAP_AND_CIRCLE_RADIUS = 1f

private const val SHADOW_RADIUS = 6f

class MyWatchFace : CanvasWatchFaceService() {

    override fun onCreateEngine(): Engine {
        return Engine()
    }

    inner class Engine : CanvasWatchFaceService.Engine() {

        private lateinit var calendar: Calendar

        private var registeredTimeZoneReceiver = false
        private var muteMode: Boolean = false
        private var centerX: Float = 0F
        private var centerY: Float = 0F

        private var sMinuteHandLength: Float = 0F
        private var sHourHandLength: Float = 0F

        /* Colors for all hands (hour, minute, ticks) based on photo loaded. */
        private var watchHandColor: Int = 0
        private var watchHandShadowColor: Int = 0

        private lateinit var hourPaint: Paint
        private lateinit var minutePaint: Paint
        private lateinit var tickAndCirclePaint: Paint

        private lateinit var backgroundPaint: Paint
        private lateinit var backgroundBitmap: Bitmap

        private var ambient: Boolean = false
        private var lowBitAmbient: Boolean = false
        private var burnInProtection: Boolean = false

        private val timeZoneReceiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                calendar.timeZone = TimeZone.getDefault()
                invalidate()
            }
        }

        override fun onCreate(holder: SurfaceHolder) {
            super.onCreate(holder)

            setWatchFaceStyle(
                WatchFaceStyle.Builder(this@MyWatchFace)
                    .setShowUnreadCountIndicator(true)
                    .build()
            )

            calendar = Calendar.getInstance()

            initializeBackground()
            initializeWatchFace()
        }

        private fun initializeBackground() {
            backgroundPaint = Paint().apply {
                color = Color.BLACK
            }
            backgroundBitmap = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
            backgroundBitmap.eraseColor(Color.parseColor("#834C9E"))

        }

        private fun initializeWatchFace() {
            /* Set defaults for colors */
            watchHandColor = Color.WHITE
            watchHandShadowColor = Color.BLACK

            hourPaint = Paint().apply {
                color = watchHandColor
                strokeWidth = HOUR_STROKE_WIDTH
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND
            }

            minutePaint = Paint().apply {
                color = watchHandColor
                strokeWidth = MINUTE_STROKE_WIDTH
                isAntiAlias = true
                strokeCap = Paint.Cap.ROUND

            }

            tickAndCirclePaint = Paint().apply {
                color = Color.parseColor("#3A2D43")
                strokeWidth = STROKE_WIDTH
                isAntiAlias = true
                style = Paint.Style.STROKE

            }
        }

        override fun onPropertiesChanged(properties: Bundle) {
            super.onPropertiesChanged(properties)
            lowBitAmbient = properties.getBoolean(
                WatchFaceService.PROPERTY_LOW_BIT_AMBIENT, false
            )
            burnInProtection = properties.getBoolean(
                WatchFaceService.PROPERTY_BURN_IN_PROTECTION, false
            )
        }

        override fun onTimeTick() {
            super.onTimeTick()
            invalidate()
        }

        override fun onAmbientModeChanged(inAmbientMode: Boolean) {
            super.onAmbientModeChanged(inAmbientMode)
            ambient = inAmbientMode

            updateWatchHandStyle()

        }

        private fun updateWatchHandStyle() {
            if (ambient) {
                hourPaint.color = Color.WHITE
                minutePaint.color = Color.WHITE
                tickAndCirclePaint.color = Color.parseColor("#3A2D43")

                hourPaint.isAntiAlias = false
                minutePaint.isAntiAlias = false
                tickAndCirclePaint.isAntiAlias = false

                hourPaint.clearShadowLayer()
                minutePaint.clearShadowLayer()
                tickAndCirclePaint.clearShadowLayer()

            } else {
                hourPaint.color = watchHandColor
                minutePaint.color = watchHandColor
                tickAndCirclePaint.color = Color.parseColor("#3A2D43")

                hourPaint.isAntiAlias = true
                minutePaint.isAntiAlias = true
                tickAndCirclePaint.isAntiAlias = true

                minutePaint.setShadowLayer(
                    SHADOW_RADIUS, 0f, 0f, watchHandShadowColor
                )

            }
        }

        override fun onInterruptionFilterChanged(interruptionFilter: Int) {
            super.onInterruptionFilterChanged(interruptionFilter)
            val inMuteMode = interruptionFilter == WatchFaceService.INTERRUPTION_FILTER_NONE

            /* Dim display in mute mode. */
            if (muteMode != inMuteMode) {
                muteMode = inMuteMode
                hourPaint.alpha = if (inMuteMode) 100 else 255
                minutePaint.alpha = if (inMuteMode) 100 else 255
                invalidate()
            }
        }

        override fun onSurfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
            super.onSurfaceChanged(holder, format, width, height)

            centerX = width / 2f
            centerY = height / 2f

            sMinuteHandLength = (centerX * 0.75).toFloat()
            sHourHandLength = (centerX * 0.5).toFloat()

            val scale = width.toFloat() / backgroundBitmap.width.toFloat()

            backgroundBitmap = Bitmap.createScaledBitmap(
                backgroundBitmap,
                (backgroundBitmap.width * scale).toInt(),
                (backgroundBitmap.height * scale).toInt(), true
            )

        }

        override fun onDraw(canvas: Canvas, bounds: Rect) {
            val now = System.currentTimeMillis()
            calendar.timeInMillis = now

            drawBackground(canvas)
            drawWatchFace(canvas)
        }

        private fun drawBackground(canvas: Canvas) {
            if (ambient && (lowBitAmbient || burnInProtection)) {
                canvas.drawColor(Color.BLACK)
            } else {
                canvas.drawBitmap(backgroundBitmap, 0f, 0f, backgroundPaint)
            }
        }

        private fun drawWatchFace(canvas: Canvas) {
            val innerTickRadius = centerX - 10
            val outerTickRadius = centerX
            for (tickIndex in 0..11) {
                val tickRot = (tickIndex.toDouble() * Math.PI * 2.0 / 12).toFloat()
                val innerX = Math.sin(tickRot.toDouble()).toFloat() * innerTickRadius
                val innerY = (-Math.cos(tickRot.toDouble())).toFloat() * innerTickRadius
                val outerX = Math.sin(tickRot.toDouble()).toFloat() * outerTickRadius
                val outerY = (-Math.cos(tickRot.toDouble())).toFloat() * outerTickRadius
                canvas.drawLine(
                    centerX + innerX, centerY + innerY,
                    centerX + outerX, centerY + outerY, tickAndCirclePaint
                )
            }

            /*
             * These calculations reflect the rotation in degrees per unit of time, e.g.,
             * 360 / 60 = 6 and 360 / 12 = 30.
             */

            val minutesRotation = calendar.get(Calendar.MINUTE) * 6f
            val hourHandOffset = calendar.get(Calendar.MINUTE) / 2f
            val hoursRotation = calendar.get(Calendar.HOUR) * 30 + hourHandOffset

            canvas.save()

            canvas.rotate(hoursRotation, centerX, centerY)
            canvas.drawLine(
                centerX,
                centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
                centerX,
                centerY - sHourHandLength,
                hourPaint
            )

            canvas.rotate(minutesRotation - hoursRotation, centerX, centerY)
            canvas.drawLine(
                centerX,
                centerY - CENTER_GAP_AND_CIRCLE_RADIUS,
                centerX,
                centerY - sMinuteHandLength,
                minutePaint
            )
            canvas.restore()
        }

        override fun onVisibilityChanged(visible: Boolean) {
            super.onVisibilityChanged(visible)

            if (visible) {
                registerReceiver()
                /* Update time zone in case it changed while we weren't visible. */
                calendar.timeZone = TimeZone.getDefault()
                invalidate()
            } else {
                unregisterReceiver()
            }
        }

        private fun registerReceiver() {
            if (registeredTimeZoneReceiver) {
                return
            }
            registeredTimeZoneReceiver = true
            val filter = IntentFilter(Intent.ACTION_TIMEZONE_CHANGED)
            this@MyWatchFace.registerReceiver(timeZoneReceiver, filter)
        }

        private fun unregisterReceiver() {
            if (!registeredTimeZoneReceiver) {
                return
            }
            registeredTimeZoneReceiver = false
            this@MyWatchFace.unregisterReceiver(timeZoneReceiver)
        }
    }
}