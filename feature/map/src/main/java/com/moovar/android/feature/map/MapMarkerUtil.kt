package com.moovar.android.feature.map

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.PathParser

object MapMarkerUtil {

    // Canonical Google Maps teardrop pin path in 44x56 coordinate space
    private const val GOOGLE_MAPS_PIN_PATH =
        "M22,2 C12.6,2 5,9.6 5,19 C5,31.5 22,55 22,55 C22,55 39,31.5 39,19 C39,9.6 31.4,2 22,2 Z"

    // Material DirectionsTransit vector path
    private const val DIRECTIONS_TRANSIT_PATH =
        "M12,2c-4.42,0 -8,0.5 -8,4v9.5C4,17.43 5.57,19 7.5,19L6,20.5v0.5h12v-0.5L16.5,19c1.93,0 3.5,-1.57 3.5,-3.5V6c0,-3.5 -3.58,-4 -8,-4zM7.5,17c-0.83,0 -1.5,-0.67 -1.5,-1.5s0.67,-1.5 1.5,-1.5 1.5,0.67 1.5,1.5 -0.67,1.5 -1.5,1.5zM11,11H6V6h5v5zm5.5,6c-0.83,0 -1.5,-0.67 -1.5,-1.5s0.67,-1.5 1.5,-1.5 1.5,0.67 1.5,1.5 -0.67,1.5 -1.5,1.5zM18,11h-5V6h5v5z"

    fun createTrainMarkerDrawable(
        context: Context,
        primaryColor: Int = Color.parseColor("#006494"),
        containerColor: Int = Color.parseColor("#D4E4F7"),
        onPrimaryColor: Int = Color.WHITE,
        onContainerColor: Int = Color.parseColor("#001E30"),
        surfaceColor: Int = Color.WHITE
    ): Drawable {
        val density = context.resources.displayMetrics.density
        val width = (44 * density).toInt()
        val height = (56 * density).toInt()

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        val centerX = width / 2f
        val tipY = 55f * density

        // Determine pin body color: ensure rich, high-contrast hue against light map tiles
        val pinBodyColor = if (ColorUtils.calculateLuminance(primaryColor) > 0.45f) {
            // Dark theme: primary is a light pastel. Use containerColor if dark, or a rich primary blend
            if (ColorUtils.calculateLuminance(containerColor) < 0.35f) {
                containerColor
            } else {
                ColorUtils.blendARGB(primaryColor, Color.BLACK, 0.45f)
            }
        } else {
            // Light theme: primary is already deep & rich
            primaryColor
        }

        // 1. GROUND CONTACT SHADOW underneath the tip
        val groundShadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(65, 0, 0, 0)
        }
        val groundRect = RectF(
            centerX - 8f * density,
            53.5f * density,
            centerX + 8f * density,
            55.5f * density
        )
        canvas.drawOval(groundRect, groundShadowPaint)

        // 2. PIN PATH SCALED BY DENSITY
        val pinPath = PathParser.createPathFromPathData(GOOGLE_MAPS_PIN_PATH)
        val scaleMatrix = Matrix().apply {
            postScale(density, density)
        }
        pinPath.transform(scaleMatrix)

        // 3. PIN AMBIENT ELEVATION DROP SHADOW
        val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(70, 0, 0, 0)
            try {
                maskFilter = BlurMaskFilter(2.2f * density, BlurMaskFilter.Blur.NORMAL)
            } catch (_: Exception) {}
        }
        val shadowMatrix = Matrix().apply {
            postTranslate(0f, 1.8f * density)
        }
        val shadowPath = Path(pinPath)
        shadowPath.transform(shadowMatrix)
        canvas.drawPath(shadowPath, shadowPaint)

        // 4. PIN BODY FILL (Material Expressive subtle vertical gradient for depth)
        val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            shader = LinearGradient(
                centerX, 2f * density,
                centerX, tipY,
                pinBodyColor,
                ColorUtils.blendARGB(pinBodyColor, Color.BLACK, 0.14f),
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawPath(pinPath, fillPaint)

        // 5. CRISP WHITE OUTER STROKE (2dp for contrast on busy map tiles)
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 1.8f * density
            strokeJoin = Paint.Join.ROUND
            color = Color.WHITE
        }
        canvas.drawPath(pinPath, strokePaint)

        // 6. OFFICIAL DIRECTIONS_TRANSIT TRAIN ICON (Centered in pin bulb at x=22dp, y=19dp)
        val bulbCenterY = 19f * density
        val iconSize = (20f * density).toInt()
        val iconLeft = (centerX - iconSize / 2f).toInt()
        val iconTop = (bulbCenterY - iconSize / 2f).toInt()

        val trainDrawable = try {
            ContextCompat.getDrawable(context, R.drawable.ic_directions_transit)?.mutate()
        } catch (_: Exception) {
            null
        }

        if (trainDrawable != null) {
            trainDrawable.setTint(Color.WHITE)
            trainDrawable.setBounds(iconLeft, iconTop, iconLeft + iconSize, iconTop + iconSize)
            trainDrawable.draw(canvas)
        } else {
            // High-fidelity fallback via PathParser
            try {
                val iconPath = PathParser.createPathFromPathData(DIRECTIONS_TRANSIT_PATH)
                val iconMatrix = Matrix().apply {
                    val scale = iconSize / 24f
                    postScale(scale, scale)
                    postTranslate(iconLeft.toFloat(), iconTop.toFloat())
                }
                iconPath.transform(iconMatrix)

                val iconPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    style = Paint.Style.FILL
                    color = Color.WHITE
                }
                canvas.drawPath(iconPath, iconPaint)
            } catch (_: Exception) {}
        }

        return BitmapDrawable(context.resources, bitmap)
    }
}
