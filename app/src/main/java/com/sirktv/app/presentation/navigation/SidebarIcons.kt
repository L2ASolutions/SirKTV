package com.sirktv.app.presentation.navigation

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Custom sidebar iconography, hand-drawn on [Canvas] at a conceptual 24x24
 * viewport — the same technique [com.sirktv.app.presentation.common.SirKTVLogoMark]
 * already uses for the brand mark. This app has zero drawable resources and
 * zero Icon/painterResource usage anywhere today; getting precise multi-path
 * geometry right (an 8-tooth gear ring, a bezier heart, hinged clapperboard
 * flap) is safer and easier to verify as Kotlin than as hand-authored
 * VectorDrawable pathData nobody here can render-preview. Every icon takes a
 * [tint] and fills whatever size its `Modifier` is given.
 */
object SirKTVIcons {

    @Composable
    fun Home(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier = modifier) {
            val s = size.width / 24f
            val stroke = Stroke(width = 1.8f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val roof = Path().apply {
                moveTo(4f * s, 11f * s)
                lineTo(12f * s, 4f * s)
                lineTo(20f * s, 11f * s)
            }
            drawPath(roof, tint, style = stroke)
            drawLine(tint, Offset(6f * s, 10.5f * s), Offset(6f * s, 20f * s), strokeWidth = 1.8f * s, cap = StrokeCap.Round)
            drawLine(tint, Offset(18f * s, 10.5f * s), Offset(18f * s, 20f * s), strokeWidth = 1.8f * s, cap = StrokeCap.Round)
            drawLine(tint, Offset(6f * s, 20f * s), Offset(18f * s, 20f * s), strokeWidth = 1.8f * s, cap = StrokeCap.Round)
            val door = Path().apply {
                moveTo(10.3f * s, 20f * s)
                lineTo(10.3f * s, 14.8f * s)
                lineTo(13.7f * s, 14.8f * s)
                lineTo(13.7f * s, 20f * s)
            }
            drawPath(door, tint, style = stroke)
        }
    }

    @Composable
    fun LiveTv(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier = modifier) {
            val s = size.width / 24f
            drawLine(tint, Offset(8f * s, 5.5f * s), Offset(6f * s, 2.5f * s), strokeWidth = 1.6f * s, cap = StrokeCap.Round)
            drawLine(tint, Offset(16f * s, 5.5f * s), Offset(18f * s, 2.5f * s), strokeWidth = 1.6f * s, cap = StrokeCap.Round)
            drawRoundRect(
                color = tint,
                topLeft = Offset(3.5f * s, 5.5f * s),
                size = Size(17f * s, 11f * s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(2f * s, 2f * s),
                style = Stroke(width = 1.8f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            drawCircle(tint, radius = 1f * s, center = Offset(17.5f * s, 8.5f * s))
            drawLine(tint, Offset(9f * s, 19.5f * s), Offset(15f * s, 19.5f * s), strokeWidth = 1.8f * s, cap = StrokeCap.Round)
            drawLine(tint, Offset(12f * s, 16.5f * s), Offset(12f * s, 19.5f * s), strokeWidth = 1.8f * s, cap = StrokeCap.Round)
        }
    }

    @Composable
    fun Movies(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier = modifier) {
            val s = size.width / 24f
            val stroke = Stroke(width = 1.7f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            drawRoundRect(
                color = tint,
                topLeft = Offset(3.5f * s, 10f * s),
                size = Size(17f * s, 9.5f * s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f * s, 1.5f * s),
                style = stroke
            )
            // Hinged-open clapperboard flap
            val flap = Path().apply {
                moveTo(3.5f * s, 10f * s)
                lineTo(4.5f * s, 5f * s)
                lineTo(20.5f * s, 5f * s)
                lineTo(20.5f * s, 10f * s)
                close()
            }
            drawPath(flap, tint, style = stroke)
            listOf(7.5f, 11f, 14.5f, 18f).forEach { x ->
                drawLine(tint, Offset(x * s, 5.4f * s), Offset((x - 1.6f) * s, 9.6f * s), strokeWidth = 1.5f * s, cap = StrokeCap.Round)
            }
        }
    }

    @Composable
    fun Series(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier = modifier) {
            val s = size.width / 24f
            drawRoundRect(
                color = tint,
                topLeft = Offset(3f * s, 4f * s),
                size = Size(12f * s, 16f * s),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * s, 3f * s),
                style = Stroke(width = 1.7f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
            val play = Path().apply {
                moveTo(8f * s, 8.5f * s)
                lineTo(14f * s, 12f * s)
                lineTo(8f * s, 15.5f * s)
                close()
            }
            drawPath(play, tint)
            drawLine(tint, Offset(17.5f * s, 9f * s), Offset(21.5f * s, 9f * s), strokeWidth = 1.7f * s, cap = StrokeCap.Round)
            drawLine(tint, Offset(17.5f * s, 15f * s), Offset(21.5f * s, 15f * s), strokeWidth = 1.7f * s, cap = StrokeCap.Round)
        }
    }

    @Composable
    fun Sports(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier = modifier) {
            val s = size.width / 24f
            val stroke = Stroke(width = 1.7f * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
            val bowl = Path().apply {
                moveTo(7f * s, 4f * s)
                lineTo(17f * s, 4f * s)
                lineTo(16f * s, 10.5f * s)
                cubicTo(16f * s, 13f * s, 14f * s, 14f * s, 12f * s, 14f * s)
                cubicTo(10f * s, 14f * s, 8f * s, 13f * s, 8f * s, 10.5f * s)
                close()
            }
            drawPath(bowl, tint, style = stroke)
            drawArc(
                tint, startAngle = 90f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(3.2f * s, 5f * s), size = Size(4f * s, 5f * s), style = stroke
            )
            drawArc(
                tint, startAngle = -90f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(16.8f * s, 5f * s), size = Size(4f * s, 5f * s), style = stroke
            )
            drawLine(tint, Offset(12f * s, 14f * s), Offset(12f * s, 17f * s), strokeWidth = 1.9f * s, cap = StrokeCap.Round)
            drawLine(tint, Offset(8.5f * s, 19.5f * s), Offset(15.5f * s, 19.5f * s), strokeWidth = 2.1f * s, cap = StrokeCap.Round)
        }
    }

    @Composable
    fun Search(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier = modifier) {
            val s = size.width / 24f
            drawCircle(
                tint,
                radius = 6f * s,
                center = Offset(10f * s, 10f * s),
                style = Stroke(width = 2.1f * s, cap = StrokeCap.Round)
            )
            drawLine(tint, Offset(14.5f * s, 14.5f * s), Offset(20f * s, 20f * s), strokeWidth = 2.4f * s, cap = StrokeCap.Round)
        }
    }

    @Composable
    fun Favorites(tint: Color, filled: Boolean, modifier: Modifier = Modifier) {
        Canvas(modifier = modifier) {
            val s = size.width / 24f
            val heart = Path().apply {
                moveTo(12f * s, 20.3f * s)
                cubicTo(6.7f * s, 16f * s, 3.2f * s, 13f * s, 3.2f * s, 9.2f * s)
                cubicTo(3.2f * s, 6.2f * s, 5.5f * s, 4f * s, 8.2f * s, 4f * s)
                cubicTo(9.8f * s, 4f * s, 11.2f * s, 4.8f * s, 12f * s, 6.1f * s)
                cubicTo(12.8f * s, 4.8f * s, 14.2f * s, 4f * s, 15.8f * s, 4f * s)
                cubicTo(18.5f * s, 4f * s, 20.8f * s, 6.2f * s, 20.8f * s, 9.2f * s)
                cubicTo(20.8f * s, 13f * s, 17.3f * s, 16f * s, 12f * s, 20.3f * s)
                close()
            }
            if (filled) {
                drawPath(heart, tint)
            } else {
                drawPath(heart, tint, style = Stroke(width = 1.7f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
        }
    }

    @Composable
    fun Settings(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier = modifier) {
            val s = size.width / 24f
            val center = Offset(12f * s, 12f * s)
            val outerR = 8.6f * s
            val innerR = 6.5f * s
            val holeR = 3.1f * s
            val teeth = 8
            val toothHalfAngle = (PI / teeth) * 0.42

            fun point(angle: Double, r: Float) = Offset(
                center.x + (r * cos(angle)).toFloat(),
                center.y + (r * sin(angle)).toFloat()
            )

            val gearTeeth = Path()
            for (i in 0 until teeth) {
                val mid = (2 * PI / teeth) * i - PI / 2
                val next = (2 * PI / teeth) * (i + 1) - PI / 2
                val a0 = point(mid - toothHalfAngle, innerR)
                val a1 = point(mid - toothHalfAngle, outerR)
                val a2 = point(mid + toothHalfAngle, outerR)
                val a3 = point(mid + toothHalfAngle, innerR)
                val a4 = point(next - toothHalfAngle, innerR)
                if (i == 0) gearTeeth.moveTo(a0.x, a0.y) else gearTeeth.lineTo(a0.x, a0.y)
                gearTeeth.lineTo(a1.x, a1.y)
                gearTeeth.lineTo(a2.x, a2.y)
                gearTeeth.lineTo(a3.x, a3.y)
                gearTeeth.lineTo(a4.x, a4.y)
            }
            gearTeeth.close()

            val hole = Path().apply { addOval(Rect(center = center, radius = holeR)) }
            val ring = Path().apply { op(gearTeeth, hole, PathOperation.Difference) }
            drawPath(ring, tint)
        }
    }

    @Composable
    fun Person(tint: Color, modifier: Modifier = Modifier) {
        Canvas(modifier = modifier) {
            val s = size.width / 24f
            drawCircle(tint, radius = 3.2f * s, center = Offset(12f * s, 7.5f * s))
            val shoulders = Path().apply {
                moveTo(5.5f * s, 20f * s)
                cubicTo(5.5f * s, 15.5f * s, 8.3f * s, 13.2f * s, 12f * s, 13.2f * s)
                cubicTo(15.7f * s, 13.2f * s, 18.5f * s, 15.5f * s, 18.5f * s, 20f * s)
                close()
            }
            drawPath(shoulders, tint)
        }
    }
}
