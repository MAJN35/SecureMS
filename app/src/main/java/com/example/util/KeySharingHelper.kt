package com.example.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color as AndroidColor
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.core.content.FileProvider
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.io.File
import java.io.FileOutputStream

object KeySharingHelper {

    const val TELEGRAM_PACKAGE = "org.telegram.messenger"
    const val BLUETOOTH_PACKAGE = "com.android.bluetooth"

    /**
     * Generates a standard ISO/IEC 18004 compliant QR Code bitmap
     * readable by any camera, Telegram QR scanner, or barcode scanner app.
     */
    fun generateQrBitmap(
        content: String,
        sizePx: Int = 512,
        foregroundColor: Int = AndroidColor.BLACK,
        backgroundColor: Int = AndroidColor.WHITE
    ): Bitmap {
        val hints = HashMap<EncodeHintType, Any>().apply {
            put(EncodeHintType.CHARACTER_SET, "UTF-8")
            put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M)
            put(EncodeHintType.MARGIN, 1)
        }

        val bitMatrix = QRCodeWriter().encode(
            content,
            BarcodeFormat.QR_CODE,
            sizePx,
            sizePx,
            hints
        )

        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)

        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix.get(x, y)) foregroundColor else backgroundColor
            }
        }

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    }

    /**
     * Formats a clean, professional public key share message.
     */
    fun buildKeyShareText(publicKeyBase64: String, fingerprint: String): String {
        return buildString {
            appendLine("🛡️ کلید عمومی امنیتی من در برنامه SecureSMS:")
            appendLine()
            appendLine(publicKeyBase64)
            appendLine()
            appendLine("اثر انگشت امنیتی (Fingerprint):")
            appendLine(fingerprint)
            appendLine()
            append("این کلید را در صفحه چت SecureSMS وارد کنید تا ارتباط کاملاً رمزنگاری‌شده فعال شود.")
        }
    }

    /**
     * Shares public key text via system share sheet (Bluetooth, Telegram, WhatsApp, Notes, etc.).
     */
    fun shareKeyText(
        context: Context,
        publicKeyBase64: String,
        fingerprint: String,
        targetPackage: String? = null
    ) {
        val shareText = buildKeyShareText(publicKeyBase64, fingerprint)

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "کلید امنیتی SecureSMS")
            putExtra(Intent.EXTRA_TEXT, shareText)
            if (targetPackage != null) {
                setPackage(targetPackage)
            }
        }

        try {
            if (targetPackage != null && isAppInstalled(context, targetPackage)) {
                context.startActivity(sendIntent)
            } else {
                val chooser = Intent.createChooser(sendIntent, "اشتراک‌گذاری کلید امنیتی (تلگرام، بلوتوث و...)")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Log.e("KeySharingHelper", "Error launching share intent: ${e.message}", e)
            // Fallback to chooser without package filter
            try {
                val fallbackIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                }
                val chooser = Intent.createChooser(fallbackIntent, "اشتراک‌گذاری کلید امنیتی")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            } catch (err: Exception) {
                Toast.makeText(context, "خطا در اشتراک‌گذاری: ${err.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Generates a standard QR image and shares it via system share sheet or target app
     * (allows sending QR code image directly to Telegram, Bluetooth, etc.).
     */
    fun shareQrCodeImage(
        context: Context,
        publicKeyBase64: String,
        fingerprint: String,
        targetPackage: String? = null
    ) {
        try {
            val bitmap = generateQrBitmap(publicKeyBase64, 600)
            val imagesFolder = File(context.cacheDir, "images")
            if (!imagesFolder.exists()) {
                imagesFolder.mkdirs()
            }

            val file = File(imagesFolder, "securesms_key_qr.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }

            val uri: Uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val shareText = buildKeyShareText(publicKeyBase64, fingerprint)

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, "کد QR کلید امنیتی SecureSMS")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (targetPackage != null) {
                    setPackage(targetPackage)
                }
            }

            if (targetPackage != null && isAppInstalled(context, targetPackage)) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                val chooser = Intent.createChooser(intent, "اشتراک تصویر QR (تلگرام، بلوتوث و...)")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)
            }
        } catch (e: Exception) {
            Log.e("KeySharingHelper", "Error sharing QR code image: ${e.message}", e)
            Toast.makeText(context, "خطا در ایجاد تصویر QR: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Share directly via Bluetooth OPP transfer.
     */
    fun shareViaBluetooth(context: Context, publicKeyBase64: String, fingerprint: String) {
        try {
            val shareText = buildKeyShareText(publicKeyBase64, fingerprint)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage(BLUETOOTH_PACKAGE)
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            if (isAppInstalled(context, BLUETOOTH_PACKAGE)) {
                context.startActivity(intent)
            } else {
                shareKeyText(context, publicKeyBase64, fingerprint)
            }
        } catch (e: Exception) {
            // Fallback to system chooser
            shareKeyText(context, publicKeyBase64, fingerprint)
        }
    }

    /**
     * Share directly via Telegram if installed, otherwise opens chooser.
     */
    fun shareViaTelegram(context: Context, publicKeyBase64: String, fingerprint: String) {
        try {
            val shareText = buildKeyShareText(publicKeyBase64, fingerprint)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                setPackage(TELEGRAM_PACKAGE)
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            if (isAppInstalled(context, TELEGRAM_PACKAGE)) {
                context.startActivity(intent)
            } else {
                shareKeyText(context, publicKeyBase64, fingerprint)
            }
        } catch (e: Exception) {
            shareKeyText(context, publicKeyBase64, fingerprint)
        }
    }

    /**
     * Checks if a specific package is installed on the device.
     */
    fun isAppInstalled(context: Context, packageName: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Extracts a pure Base64 public key from messy text (e.g. copied from Telegram or notes).
     */
    fun extractBase64Key(rawInput: String): String {
        val trimmed = rawInput.trim()
        // If whole input is valid Base64
        val lines = trimmed.lines()
        for (line in lines) {
            val clean = line.trim()
            if (clean.length in 80..180 && clean.matches(Regex("^[A-Za-z0-9+/=]+$"))) {
                return clean
            }
        }
        // Try to find regex match for Base64 public key
        val match = Regex("[A-Za-z0-9+/]{80,}={0,2}").find(trimmed)
        if (match != null) {
            return match.value
        }
        return trimmed
    }
}
