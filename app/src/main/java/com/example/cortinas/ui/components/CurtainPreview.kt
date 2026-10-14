package com.example.cortinas.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Brightness5
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.ViewDay
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cortinas.data.model.Tela
import com.example.cortinas.util.FormatUtils

@Composable
fun CurtainPreview(
    ancho: Double,
    alto: Double,
    tela: Tela?,
    mandoLado: String,
    esDosCuerpos: Boolean = false,
    anchoCuerpoIzq: Double = 0.0,
    anchoCuerpoDer: Double = 0.0,
    modifier: Modifier = Modifier
) {
    // Proporción animada suavemente con Spring
    val targetRatio = remember(ancho, alto) {
        if (ancho > 0 && alto > 0) {
            (ancho / alto).toFloat().coerceIn(0.45f, 2.2f)
        } else {
            1.2f
        }
    }

    val animatedRatio by animateFloatAsState(
        targetValue = targetRatio,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ratioAnimation"
    )

    // Animación suave del área calculada
    val areaM2 = (ancho * alto)

    // Colores según tipo de tela
    val tipo = tela?.tipo ?: "Translúcida"
    val isBlackout = tipo.contains("Blackout", ignoreCase = true)
    val isZebra = tipo.contains("Zebra", ignoreCase = true) || tipo.contains("Sheer", ignoreCase = true)
    val isScreen = tipo.contains("Screen", ignoreCase = true)

    val fabricColor = when {
        isBlackout -> Color(0xFF1E293B)
        isScreen -> Color(0xFF64748B)
        isZebra -> Color(0xFF475569)
        else -> Color(0xFFCBD5E1)
    }

    val animatedFabricColor by animateColorAsState(
        targetValue = fabricColor,
        animationSpec = tween(500),
        label = "fabricColor"
    )

    // Efecto sutil de brillo para la luz solar de fondo
    val infiniteTransition = rememberInfiniteTransition(label = "sunGlow")
    val sunPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sunPulse"
    )

    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header con etiquetas de tipo y controles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val (icon, labelText) = when {
                        isBlackout -> Pair(Icons.Default.DarkMode, "Bloqueo Solar 100%")
                        isZebra -> Pair(Icons.Default.ViewDay, "Doble Franja Noche & Día")
                        isScreen -> Pair(Icons.Default.GridOn, "Filtro Micro-perforado")
                        else -> Pair(Icons.Default.Brightness5, "Paso de Luz Suave")
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (esDosCuerpos) "$labelText • 2 Cuerpos" else labelText,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.padding(2.dp)
                ) {
                    Text(
                        text = if (areaM2 > 0) FormatUtils.formatM2(areaM2) else "0.00 m²",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // VENTANA Y SIMULADOR DE CORTINA EN CANVAS
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFE2E8F0),
                                Color(0xFFF1F5F9),
                                Color(0xFFE2E8F0)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // Dimensiones del lienzo de la cortina según el ratio
                    val maxCurtainW = canvasWidth * 0.85f
                    val maxCurtainH = canvasHeight * 0.80f

                    var curtainW = maxCurtainW
                    var curtainH = curtainW / animatedRatio

                    if (curtainH > maxCurtainH) {
                        curtainH = maxCurtainH
                        curtainW = curtainH * animatedRatio
                    }

                    val left = (canvasWidth - curtainW) / 2f
                    val top = 22f

                    // 1. Luz de fondo / Ventana detrás de la cortina
                    val winLeft = left - 8f
                    val winTop = top
                    val winW = curtainW + 16f
                    val winH = curtainH + 10f

                    // Marco de ventana exterior
                    drawRoundRect(
                        color = Color(0xFFCBD5E1),
                        topLeft = Offset(winLeft, winTop),
                        size = Size(winW, winH),
                        cornerRadius = CornerRadius(10f, 10f),
                        style = Stroke(width = 6f)
                    )

                    // Resplandor de cielo/sol de ventana
                    drawRoundRect(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color(0xFFFFFBEB).copy(alpha = if (isBlackout) 0.15f else 0.85f * sunPulse),
                                Color(0xFFBAE6FD).copy(alpha = if (isBlackout) 0.10f else 0.65f)
                            ),
                            center = Offset(canvasWidth / 2f, top + curtainH / 2f),
                            radius = curtainW * 0.9f
                        ),
                        topLeft = Offset(winLeft, winTop),
                        size = Size(winW, winH),
                        cornerRadius = CornerRadius(10f, 10f)
                    )

                    // 2. Tubo superior / Cabezal de la cortina enrollable
                    val tubeH = 16f
                    val tubeW = curtainW + 24f
                    val tubeLeft = left - 12f

                    // Soportes laterales del tubo
                    drawRoundRect(
                        color = Color(0xFF475569),
                        topLeft = Offset(tubeLeft - 4f, top - 2f),
                        size = Size(10f, tubeH + 4f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )
                    drawRoundRect(
                        color = Color(0xFF475569),
                        topLeft = Offset(tubeLeft + tubeW - 6f, top - 2f),
                        size = Size(10f, tubeH + 4f),
                        cornerRadius = CornerRadius(4f, 4f)
                    )

                    // Tubo de aluminio enrollable
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFFE2E8F0),
                                Color(0xFF94A3B8),
                                Color(0xFF475569),
                                Color(0xFFCBD5E1)
                            ),
                            startY = top,
                            endY = top + tubeH
                        ),
                        topLeft = Offset(tubeLeft, top),
                        size = Size(tubeW, tubeH),
                        cornerRadius = CornerRadius(4f, 4f)
                    )

                    // 3. Tela de la cortina (1 Cuerpo o 2 Cuerpos)
                    val fabricAlpha = if (isBlackout) 0.98f else if (isScreen) 0.88f else 0.72f
                    val fabricTop = top + tubeH - 2f

                    if (!esDosCuerpos) {
                        // UN SOLO CUERPO COMPLETO
                        drawRoundRect(
                            color = animatedFabricColor.copy(alpha = fabricAlpha),
                            topLeft = Offset(left, fabricTop),
                            size = Size(curtainW, curtainH),
                            cornerRadius = CornerRadius(2f, 2f)
                        )

                        // Sombra en la caída
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Black.copy(alpha = 0.25f), Color.Transparent),
                                startY = fabricTop,
                                endY = fabricTop + 20f
                            ),
                            topLeft = Offset(left, fabricTop),
                            size = Size(curtainW, 20f)
                        )

                        // Perfil Inferior
                        val bottomBarH = 10f
                        val bottomBarY = fabricTop + curtainH
                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFF1F5F9), Color(0xFF64748B), Color(0xFF334155)),
                                startY = bottomBarY,
                                endY = bottomBarY + bottomBarH
                            ),
                            topLeft = Offset(left - 2f, bottomBarY),
                            size = Size(curtainW + 4f, bottomBarH),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                    } else {
                        // DOS CUERPOS (IZQUIERDO Y DERECHO CON SEPARACIÓN VISUAL)
                        val gap = 5f
                        val ratioIzq = if (ancho > 0 && anchoCuerpoIzq > 0) {
                            (anchoCuerpoIzq / ancho).toFloat().coerceIn(0.15f, 0.85f)
                        } else 0.5f

                        val wIzq = (curtainW - gap) * ratioIzq
                        val wDer = (curtainW - gap) * (1f - ratioIzq)
                        val xDer = left + wIzq + gap

                        // Paño Izquierdo
                        drawRoundRect(
                            color = animatedFabricColor.copy(alpha = fabricAlpha),
                            topLeft = Offset(left, fabricTop),
                            size = Size(wIzq, curtainH),
                            cornerRadius = CornerRadius(2f, 2f)
                        )

                        // Paño Derecho
                        drawRoundRect(
                            color = animatedFabricColor.copy(alpha = fabricAlpha),
                            topLeft = Offset(xDer, fabricTop),
                            size = Size(wDer, curtainH),
                            cornerRadius = CornerRadius(2f, 2f)
                        )

                        // Línea divisoria central sutil
                        drawRect(
                            color = Color(0xFF334155).copy(alpha = 0.4f),
                            topLeft = Offset(left + wIzq, fabricTop),
                            size = Size(gap, curtainH)
                        )

                        // Perfiles inferiores independientes
                        val bottomBarH = 10f
                        val bottomBarY = fabricTop + curtainH

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFF1F5F9), Color(0xFF64748B), Color(0xFF334155)),
                                startY = bottomBarY,
                                endY = bottomBarY + bottomBarH
                            ),
                            topLeft = Offset(left - 1f, bottomBarY),
                            size = Size(wIzq + 2f, bottomBarH),
                            cornerRadius = CornerRadius(2f, 2f)
                        )

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFFF1F5F9), Color(0xFF64748B), Color(0xFF334155)),
                                startY = bottomBarY,
                                endY = bottomBarY + bottomBarH
                            ),
                            topLeft = Offset(xDer - 1f, bottomBarY),
                            size = Size(wDer + 2f, bottomBarH),
                            cornerRadius = CornerRadius(2f, 2f)
                        )
                    }

                    // Texturas particulares según el tipo
                    if (isZebra) {
                        val stripeCount = 10
                        val stripeH = curtainH / (stripeCount * 2)
                        for (i in 0 until stripeCount * 2 step 2) {
                            val stripeY = fabricTop + (i * stripeH)
                            if (stripeY + stripeH <= fabricTop + curtainH) {
                                drawRect(
                                    color = Color.White.copy(alpha = 0.55f),
                                    topLeft = Offset(left, stripeY),
                                    size = Size(curtainW, stripeH)
                                )
                            }
                        }
                    } else if (isScreen) {
                        val step = 14f
                        var lineY = fabricTop + step
                        while (lineY < fabricTop + curtainH) {
                            drawLine(
                                color = Color.White.copy(alpha = 0.18f),
                                start = Offset(left, lineY),
                                end = Offset(left + curtainW, lineY),
                                strokeWidth = 2f
                            )
                            lineY += step
                        }
                    }

                    // 4. Cadena de mando (Derecha o Izquierda)
                    val isRightSide = mandoLado.contains("Derecha", ignoreCase = true)
                    val chainX = if (isRightSide) tubeLeft + tubeW - 2f else tubeLeft + 2f
                    val chainH = curtainH * 0.75f

                    val beadSpacing = 8f
                    var beadY = top + tubeH
                    while (beadY < top + tubeH + chainH) {
                        drawCircle(
                            color = Color(0xFF94A3B8),
                            radius = 2.2f,
                            center = Offset(chainX, beadY)
                        )
                        beadY += beadSpacing
                    }
                    drawArc(
                        color = Color(0xFF94A3B8),
                        startAngle = 0f,
                        sweepAngle = 180f,
                        useCenter = false,
                        topLeft = Offset(chainX - 4f, beadY - 4f),
                        size = Size(8f, 8f),
                        style = Stroke(width = 2f)
                    )
                }

                // Indicador flotante de medidas
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 8.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.70f))
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (esDosCuerpos) {
                        Text(
                            text = "2 Cuerpos: Izq ${FormatUtils.formatNumber(anchoCuerpoIzq)}m + Der ${FormatUtils.formatNumber(anchoCuerpoDer)}m",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("  =  ", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(
                            text = "${FormatUtils.formatNumber(ancho)}m",
                            color = Color(0xFF4ADE80),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    } else {
                        Text(
                            text = "↔ ${FormatUtils.formatNumber(ancho)}m",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text("  •  ", color = Color(0xFF94A3B8), fontSize = 11.sp)
                        Text(
                            text = "↕ ${FormatUtils.formatNumber(alto)}m",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
