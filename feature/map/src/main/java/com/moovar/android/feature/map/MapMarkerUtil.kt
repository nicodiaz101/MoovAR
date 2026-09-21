package com.moovar.android.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.graphics.ColorUtils
import kotlin.math.cos
import kotlin.math.sin

object MapMarkerUtil {

    fun createTrainMarkerDrawable(
        context: Context,
        primaryColor: Int = Color.parseColor("#006494"),
        containerColor: Int = Color.parseColor("#D4E4F7"),
        onPrimaryColor: Int = Color.WHITE,
        onContainerColor: Int = Color.parseColor("#001E30"),
        surfaceColor: Int = Color.WHITE
    ): Drawable {
        val density = context.resources.displayMetrics.density
        val width = (56 * density).toInt()
        val height = (72 * density).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val centerX = width / 2f
        val bulbRadius = 20f * density
        val bulbCenterY = bulbRadius + 4f * density
        val groundY = height - 4f * density
        val tipY = groundY - 2f * density

        // 1. MATERIAL EXPRESSIVE LIVE RADAR / BEACON PULSE AT GROUND
        val outerPulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = ColorUtils.setAlphaComponent(primaryColor, 35)
        }
        val midPulsePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = ColorUtils.setAlphaComponent(primaryColor, 80)
        }
        val groundPointPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = primaryColor
        }
        val groundPointStroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * density
            color = Color.WHITE
        }

        // Draw ground radar rings
        canvas.drawOval(
            RectF(centerX - 14f * density, groundY - 4f * density, centerX + 14f * density, groundY + 4f * density),
            outerPulsePaint
        )
        canvas.drawOval(
            RectF(centerX - 8f * density, groundY - 2.5f * density, centerX + 8f * density, groundY + 2.5f * density),
            midPulsePaint
        )
        canvas.drawOval(
            RectF(centerX - 3f * density, groundY - 1.2f * density, centerX + 3f * density, groundY + 1.2f * density),
            groundPointPaint
        )
        canvas.drawOval(
            RectF(centerX - 3f * density, groundY - 1.2f * density, centerX + 3f * density, groundY + 1.2f * density),
            groundPointStroke
        )

        // 2. DROPLET PIN AMBIENT SHADOW
        val shadowPath = Path()
        val shadowOffset = 3f * density
        buildDropletPath(shadowPath, centerX, bulbCenterY + shadowOffset, bulbRadius, tipY + shadowOffset)
        val ambientShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(45, 0, 0, 0)
        }
        canvas.drawPath(shadowPath, ambientShadowPaint)

        // 3. EXPRESSIVE DROPLET PIN BODY WITH DYNAMIC GRADIENT
        val pinPath = Path()
        buildDropletPath(pinPath, centerX, bulbCenterY, bulbRadius, tipY)

        val darkerPrimary = ColorUtils.blendARGB(primaryColor, Color.BLACK, 0.15f)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                centerX, bulbCenterY - bulbRadius,
                centerX, tipY,
                primaryColor, darkerPrimary,
                Shader.TileMode.CLAMP
            )
        }

        val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 2.2f * density
            color = Color.WHITE
        }

        canvas.drawPath(pinPath, fillPaint)
        canvas.drawPath(pinPath, outlinePaint)

        // 4. INNER CONTAINER DISC
        val innerRadius = bulbRadius * 0.72f
        val innerDiscPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = surfaceColor
        }
        val innerBorderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.2f * density
            color = ColorUtils.setAlphaComponent(primaryColor, 40)
        }

        canvas.drawCircle(centerX, bulbCenterY, innerRadius, innerDiscPaint)
        canvas.drawCircle(centerX, bulbCenterY, innerRadius, innerBorderPaint)

        // 5. TRAIN GLYPH (Material Expressive icon)
        val trainPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = primaryColor
        }
        val windowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = surfaceColor
        }
        val headlightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.parseColor("#FFD54F") // Warm luminous train headlights
        }
        val railPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.4f * density
            color = primaryColor
            strokeCap = Paint.Cap.ROUND
        }

        val trainW = innerRadius * 1.18f
        val trainH = innerRadius * 1.32f
        val trainLeft = centerX - trainW / 2f
        val trainTop = bulbCenterY - trainH / 2f
        val trainRight = centerX + trainW / 2f
        val trainBottom = bulbCenterY + trainH / 2f

        // Train main body
        val cornerR = 3.5f * density
        val bodyRect = RectF(trainLeft, trainTop, trainRight, trainBottom - 2.5f * density)
        canvas.drawRoundRect(bodyRect, cornerR, cornerR, trainPaint)

        // Train dual panoramic windshield
        val winMarginH = 2f * density
        val winTop = trainTop + 2.5f * density
        val winBottom = trainTop + trainH * 0.44f
        val winRect = RectF(trainLeft + winMarginH, winTop, trainRight - winMarginH, winBottom)
        canvas.drawRoundRect(winRect, 1.8f * density, 1.8f * density, windowPaint)

        // Windshield center pillar
        val pillarW = 1.4f * density
        canvas.drawRect(centerX - pillarW / 2f, winTop, centerX + pillarW / 2f, winBottom, trainPaint)

        // Warm dual headlights
        val lightR = 1.4f * density
        val lightY = bodyRect.bottom - 2.8f * density
        canvas.drawCircle(trainLeft + 3.2f * density, lightY, lightR, headlightPaint)
        canvas.drawCircle(trainRight - 3.2f * density, lightY, lightR, headlightPaint)

        // Rail line underneath
        val railY = trainBottom + 0.5f * density
        val railW = trainW * 0.95f
        canvas.drawLine(centerX - railW / 2f, railY, centerX + railW / 2f, railY, railPaint)

        return BitmapDrawable(context.resources, bitmap)
    }

    private fun buildDropletPath(
        path: Path,
        centerX: Float,
        bulbCenterY: Float,
        bulbRadius: Float,
        tipY: Float
    ) {
        path.reset()
        val arcRect = RectF(
            centerX - bulbRadius,
            bulbCenterY - bulbRadius,
            centerX + bulbRadius,
            bulbCenterY + bulbRadius
        )
        // Sweep arc from 38 degrees through top to 142 degrees
        path.arcTo(arcRect, 38f, 264f, false)

        val rad38 = Math.toRadians(38.0)
        val arcRightX = centerX + bulbRadius * cos(rad38).toFloat()
        val arcRightY = bulbCenterY + bulbRadius * sin(rad38).toFloat()

        // Left curve from arc edge down to tip
        val ctrlX1 = centerX - bulbRadius * 0.85f
        val ctrlY1 = bulbCenterY + bulbRadius * 0.7f
        val ctrlX2 = centerX - 2.5f
        val ctrlY2 = tipY - 4f
        path.cubicTo(ctrlX1, ctrlY1, ctrlX2, ctrlY2, centerX, tipY)

        // Right curve from tip back to right arc edge
        val ctrlX3 = centerX + 2.5f
        val ctrlY3 = tipY - 4f
        val ctrlX4 = centerX + bulbRadius * 0.85f
        val ctrlY4 = bulbCenterY + bulbRadius * 0.7f
        path.cubicTo(ctrlX3, ctrlY3, ctrlX4, ctrlY4, arcRightX, arcRightY)

        path.close()
    }
}
