package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/**
 * Renders an ISO/IEC 18004 compliant, scannable QR Code using ZXing and Jetpack Compose Canvas
 * inside a rounded translucent glass frame (no harsh solid rectangle backgrounds).
 */
@Composable
fun QrCodeCanvas(
    contentString: String,
    modifier: Modifier = Modifier,
    matrixColor: Color = Color.Unspecified,
    backgroundColor: Color = Color.Unspecified
) {
    val isLight = MaterialTheme.colorScheme.surface.luminance() > 0.5f
    val resolvedMatrixColor = if (matrixColor != Color.Unspecified) {
        matrixColor
    } else if (isLight) {
        Color(0xFF0052A3)
    } else {
        Color(0xFF00E5A3)
    }

    val resolvedTint = if (backgroundColor != Color.Unspecified) {
        backgroundColor
    } else if (isLight) {
        Color.White.copy(alpha = 0.88f)
    } else {
        Color(0xFF0D182C).copy(alpha = 0.72f)
    }

    val matrix = remember(contentString) {
        generateQrMatrix(contentString)
    }

    GlassCard(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(22.dp),
        cornerRadius = 22.dp,
        tintColor = resolvedTint,
        borderTint = resolvedMatrixColor.copy(alpha = 0.45f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val n = matrix.size
                if (n > 0) {
                    val cellSize = size.width / n
                    val cr = CornerRadius(cellSize * 0.28f, cellSize * 0.28f)

                    for (row in 0 until n) {
                        for (col in 0 until n) {
                            if (matrix[row][col]) {
                                drawRoundRect(
                                    color = resolvedMatrixColor,
                                    topLeft = Offset(col * cellSize, row * cellSize),
                                    size = Size(cellSize + 0.4f, cellSize + 0.4f),
                                    cornerRadius = cr
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun generateQrMatrix(input: String): Array<BooleanArray> {
    if (input.isBlank()) {
        return Array(21) { BooleanArray(21) }
    }
    return try {
        val hints = HashMap<EncodeHintType, Any>().apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
            put(EncodeHintType.MARGIN, 1)
        }
        val bitMatrix = QRCodeWriter().encode(input, BarcodeFormat.QR_CODE, 0, 0, hints)
        val w = bitMatrix.width
        val h = bitMatrix.height
        Array(h) { y ->
            BooleanArray(w) { x ->
                bitMatrix.get(x, y)
            }
        }
    } catch (e: Exception) {
        Array(21) { BooleanArray(21) { (it % 2 == 0) } }
    }
}
