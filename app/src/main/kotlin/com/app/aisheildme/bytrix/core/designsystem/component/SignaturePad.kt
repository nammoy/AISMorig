package com.app.aisheildme.bytrix.core.designsystem.component

import android.graphics.Bitmap
import android.graphics.Canvas as AndroidCanvas
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.app.aisheildme.bytrix.core.designsystem.AppColors

@Composable
fun SignaturePad(
    onSignatureChange: (ByteArray?) -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 160.dp,
) {
    val strokes = remember { mutableStateListOf<List<Offset>>() }
    var current by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var canvasWidth by remember { mutableStateOf(0) }
    var canvasHeight by remember { mutableStateOf(0) }

    LaunchedEffect(Unit) {
        snapshotFlow { strokes.size }.collect { count ->
            onSignatureChange(
                if (count == 0 || canvasWidth == 0) {
                    null
                } else {
                    renderPng(strokes, canvasWidth, canvasHeight)
                },
            )
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(height)
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .border(1.dp, AppColors.LightBorder, RoundedCornerShape(12.dp)),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { offset -> current = listOf(offset) },
                            onDrag = { change, _ ->
                                current = current + change.position
                                change.consume()
                            },
                            onDragEnd = {
                                if (current.size > 1) strokes.add(current)
                                current = emptyList()
                            },
                            onDragCancel = { current = emptyList() },
                        )
                    },
            ) {
                canvasWidth = size.width.toInt()
                canvasHeight = size.height.toInt()

                (strokes + listOf(current)).forEach { points ->
                    if (points.size < 2) return@forEach
                    val path = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        points.drop(1).forEach { lineTo(it.x, it.y) }
                    }
                    drawPath(
                        path = path,
                        color = Color.Black,
                        style = Stroke(
                            width = 2.5.dp.toPx(),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round,
                        ),
                    )
                }
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            TextButton(
                onClick = {
                    strokes.clear()
                    current = emptyList()
                    onSignatureChange(null)
                },
            ) {
                Text(
                    text = "Clear",
                    style = MaterialTheme.typography.labelMedium,
                    color = AppColors.TextHint,
                )
            }
        }
    }
}

private fun renderPng(strokes: List<List<Offset>>, width: Int, height: Int): ByteArray? {
    if (width <= 0 || height <= 0) return null

    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = AndroidCanvas(bitmap)
    canvas.drawColor(android.graphics.Color.WHITE)

    val paint = Paint().apply {
        color = android.graphics.Color.BLACK
        strokeWidth = 5f
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        isAntiAlias = true
    }

    strokes.forEach { points ->
        if (points.size < 2) return@forEach
        val path = AndroidPath().apply {
            moveTo(points.first().x, points.first().y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        canvas.drawPath(path, paint)
    }

    return java.io.ByteArrayOutputStream().use { out ->
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        bitmap.recycle()
        out.toByteArray()
    }
}
