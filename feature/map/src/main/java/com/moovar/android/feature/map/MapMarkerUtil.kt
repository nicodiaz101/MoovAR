package com.moovar.android.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable

object MapMarkerUtil {
    fun createTrainMarkerDrawable(
        context: Context,
        pinColor: Int = Color.parseColor("#0288D1")
    ): Drawable {
        val density = context.resources.displayMetrics.density
        val width = (48 * density).toInt()
        val height = (60 * density).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = pinColor
        }

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.5f * density
            color = Color.WHITE
        }

        val centerX = width / 2f
        val headRadius = (width - 12 * density) / 2f
        val headCenterY = headRadius + 4 * density
        val tipY = height - 4 * density

        // Teardrop pin path
        val path = Path()
        val arcRect = RectF(
            centerX - headRadius,
            headCenterY - headRadius,
            centerX + headRadius,
            headCenterY + headRadius
        )
        // Sweep arc for round head
        path.arcTo(arcRect, 35f, 290f, false)
        // Draw sharp tip pointing down at the center
        path.lineTo(centerX, tipY)
        path.close()

        // Shadow / base circle under the tip
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(60, 0, 0, 0)
        }
        canvas.drawOval(
            RectF(centerX - 6 * density, tipY - 1 * density, centerX + 6 * density, tipY + 3 * density),
            shadowPaint
        )

        // Draw pin background & white border
        canvas.drawPath(path, fillPaint)
        canvas.drawPath(path, strokePaint)

        // Inner white circle
        val innerCirclePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        val innerRadius = headRadius * 0.72f
        canvas.drawCircle(centerX, headCenterY, innerRadius, innerCirclePaint)

        // Train silhouette drawn inside in pinColor
        val trainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = pinColor
        }
        val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.WHITE
        }
        val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * density
            color = pinColor
        }

        val trainW = innerRadius * 1.15f
        val trainH = innerRadius * 1.35f
        val trainLeft = centerX - trainW / 2f
        val trainTop = headCenterY - trainH / 2f
        val trainRight = centerX + trainW / 2f
        val trainBottom = headCenterY + trainH / 2f

        // Train main body
        val cornerR = 3.5f * density
        val bodyRect = RectF(trainLeft, trainTop, trainRight, trainBottom - 2.5f * density)
        canvas.drawRoundRect(bodyRect, cornerR, cornerR, trainPaint)

        // Train windshield
        val winMarginH = 2f * density
        val winTop = trainTop + 2.5f * density
        val winBottom = trainTop + trainH * 0.42f
        val winRect = RectF(trainLeft + winMarginH, winTop, trainRight - winMarginH, winBottom)
        canvas.drawRoundRect(winRect, 1.5f * density, 1.5f * density, whitePaint)

        // Windshield center pillar
        val pillarW = 1.2f * density
        canvas.drawRect(centerX - pillarW / 2f, winTop, centerX + pillarW / 2f, winBottom, trainPaint)

        // Two headlights
        val lightR = 1.4f * density
        val lightY = bodyRect.bottom - 2.8f * density
        canvas.drawCircle(trainLeft + 3.2f * density, lightY, lightR, whitePaint)
        canvas.drawCircle(trainRight - 3.2f * density, lightY, lightR, whitePaint)

        // Track line
        val railY = trainBottom + 0.5f * density
        val railW = trainW * 0.9f
        canvas.drawLine(centerX - railW / 2f, railY, centerX + railW / 2f, railY, trackPaint)

        return BitmapDrawable(context.resources, bitmap)
    }
}
