package com.popcorntime.android.ui.components

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter

@Composable
fun QrCodeImage(
    content: String,
    modifier: Modifier = Modifier,
    sizePx: Int = 512,
) {
    val bitmap = remember(content, sizePx) {
        if (content.isBlank()) null else encodeQrBitmap(content, sizePx)
    }
    bitmap?.let {
        Image(
            bitmap = it.asImageBitmap(),
            contentDescription = "QR code",
            modifier = modifier,
        )
    }
}

private fun encodeQrBitmap(content: String, sizePx: Int): Bitmap? {
    return try {
        val hints = mapOf(EncodeHintType.MARGIN to 1)
        val matrix = QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
        val width = matrix.width
        val height = matrix.height
        val pixels = IntArray(width * height) { index ->
            val x = index % width
            val y = index / width
            if (matrix[x, y]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        }
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).apply {
            setPixels(pixels, 0, width, 0, 0, width, height)
        }
    } catch (_: Exception) {
        null
    }
}
