package com.example.procard.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import com.example.procard.model.DayColor
import com.example.procard.model.ProgressSnapshot
import java.time.LocalDate
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * Gráfico circular reutilizable para mostrar los últimos 28 días de progreso.
 */
@Composable
fun ProgressCircleLast28Days(
    today: LocalDate,
    snapshot: ProgressSnapshot,
    isDark: Boolean,
    onRingSelected: (LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val pulse = remember { Animatable(1f) }
    LaunchedEffect(snapshot.dayStatuses) {
        pulse.snapTo(0.94f)
        pulse.animateTo(1f, animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing))
    }

    val days: List<LocalDate> = remember(today) { (0..27).map { today.minusDays(it.toLong()) } }

    val animFractions: List<Float> = days.map {
        val value by animateFloatAsState(
            targetValue = 1f,
            animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
            label = "ringFraction"
        )
        value
    }

    BoxWithConstraints(
        modifier = modifier
            .semantics { contentDescription = "Gráfico de 28 anillos (coronas concéntricas)" }
    ) {
        val density = LocalDensity.current
        val outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f)
        val gapPx = with(density) { 2.dp.toPx() }
        val paddingPx = with(density) { 16.dp.toPx() }
        val totalRings = 28

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            val minDim = min(size.width, size.height)
            val centerX = size.width / 2f
            val centerY = size.height / 2f
            val availableRadius = (minDim / 2f - paddingPx) * pulse.value

            val ringThickness = ((availableRadius - gapPx * (totalRings - 1)) / totalRings)
                .coerceAtLeast(with(density) { 6.dp.toPx() })

            // Disco central (hoy)
            run {
                val date = days.first()
                val status = snapshot.dayStatuses[date] ?: DayColor.ROJO
                val fillColor = status.resolveColor(isDark)
                val radius = ringThickness

                drawCircle(
                    color = fillColor.copy(alpha = animFractions.first()),
                    radius = radius,
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = outlineColor,
                    radius = radius,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = with(density) { 1.dp.toPx() })
                )
            }

            // Anillos 1..27
            for (index in 1 until totalRings) {
                val date = days[index]
                val status = snapshot.dayStatuses[date] ?: DayColor.ROJO
                val color = status.resolveColor(isDark).copy(alpha = animFractions[index])

                val innerRadius = index * (ringThickness + gapPx)
                val radius = innerRadius + ringThickness / 2f

                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(centerX - radius, centerY - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = Stroke(width = ringThickness, cap = StrokeCap.Butt)
                )

                val borderRadius = radius + ringThickness / 2f
                drawArc(
                    color = outlineColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    topLeft = Offset(centerX - borderRadius, centerY - borderRadius),
                    size = Size(borderRadius * 2f, borderRadius * 2f),
                    style = Stroke(width = with(density) { 1.dp.toPx() }, cap = StrokeCap.Butt)
                )

                if (snapshot.notes.containsKey(date)) {
                    val theta = (-90f * (PI / 180f)).toFloat()
                    val inner = radius - ringThickness / 2f + ringThickness * 0.2f
                    val outer = radius + ringThickness / 2f - ringThickness * 0.2f
                    val x1 = centerX + inner * cos(theta)
                    val y1 = centerY + inner * sin(theta)
                    val x2 = centerX + outer * cos(theta)
                    val y2 = centerY + outer * sin(theta)
                    drawLine(
                        color = outlineColor,
                        start = Offset(x1, y1),
                        end = Offset(x2, y2),
                        strokeWidth = with(density) { 2.dp.toPx() },
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}
